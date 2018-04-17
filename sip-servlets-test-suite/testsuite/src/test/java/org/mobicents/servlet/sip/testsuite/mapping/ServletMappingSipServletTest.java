/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.testsuite.mapping;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;
import static junit.framework.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.NetworkPortAssigner;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class ServletMappingSipServletTest extends SipServletTestCase {

    private static transient Logger logger = Logger.getLogger(ServletMappingSipServletTest.class);

    private static final String TRANSPORT = "udp";
    private static final boolean AUTODIALOG = true;
    private static final int TIMEOUT = 10000;
//	private static final int TIMEOUT = 100000000;

    TestSipListener sender;

    ProtocolObjects senderProtocolObjects;

    private static final String[] MESSAGES = new String[]{
        "servletInitialized",
        "inviteReceived"
    };

    public ServletMappingSipServletTest(String name) {
        super(name);
        autoDeployOnStartup = false;
    }

    @Override
    public void deployApplication() {
    }

    public SipStandardContext deployApplication(Map<String, String> params) {
        SipStandardContext ctx = deployApplication(
                projectHome + "/sip-servlets-test-suite/applications/servlet-mapping-servlet/src/main/sipapp",
                "sip-test",
                params,
                null);
        assertTrue(ctx.getAvailable());
        return ctx;
    }

    @Override
    protected String getDarConfigurationFile() {
        return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/"
                + "org/mobicents/servlet/sip/testsuite/mapping/servlet-mapping-sip-servlet-dar.properties";
    }

    @Override
    protected void setUp() throws Exception {
        containerPort = NetworkPortAssigner.retrieveNextPort();
        super.setUp();

        senderProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + containerPort, null, null);
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, true);
        SipProvider senderProvider = sender.createProvider();

        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();

        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(senderPort));
        params.put("senderPort", String.valueOf(senderPort));
        deployApplication(params);
    }

    public void testServletMappingOK() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        toAddress.setParameter("foo", "fighter");
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        for (int i = 0; i < MESSAGES.length; i++) {
            assertTrue(sender.getAllMessagesContent().contains(MESSAGES[i]));
        }
        assertEquals(200, sender.getFinalResponseStatus());
        assertTrue(sender.getOkToByeReceived());
    }

    public void testServletMappingHostKO() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping";
        String toSipAddress = "mobicents2.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        toAddress.setParameter("foo", "fighter");
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(404, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());
    }

    public void testServletMappingUserKO() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "receiver";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        toAddress.setParameter("foo", "fighter");
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(404, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());
    }

    public void testServletMappingParamKO() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(404, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());
    }

    public void testServletSecondMappingOK() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping2";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(410, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());
    }

    public void testServletMappingsInTurn() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        toAddress.setParameter("foo", "fighter");
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(200, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());

        toUser = "servlet-mapping2";
        toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(410, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());

        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        for (int i = 0; i < MESSAGES.length; i++) {
            assertTrue(sender.getAllMessagesContent().contains(MESSAGES[i]));
        }
    }

    public void testServletSecondMappingKO() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
        String fromName = "sender";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromSipAddress);

        String toUser = "servlet-mapping3";
        String toSipAddress = "mobicents.sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toSipAddress);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
        Thread.sleep(TIMEOUT);
        assertEquals(404, sender.getFinalResponseStatus());
        assertTrue(sender.isFinalResponseReceived());
    }

    @Override
    protected void tearDown() throws Exception {
        senderProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }

}
