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
package org.mobicents.servlet.sip.testsuite.session;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sip.SipProvider;
import static junit.framework.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.NetworkPortAssigner;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class SipAppSessionTerminationTest extends SipServletTestCase {

    private String CLICK2DIAL_URL;
    private String CLICK2DIAL_PARAMS;
    private static transient Logger logger = Logger.getLogger(SipAppSessionTerminationTest.class);

    TestSipListener receiver;
    ProtocolObjects receiverProtocolObjects;
    private static final String TRANSPORT = "udp";
    private static final boolean AUTODIALOG = true;

    private static final int TIMEOUT = 10000;

    public SipAppSessionTerminationTest(String name) {
        super(name);
        autoDeployOnStartup = false;
    }

    @Override
    public void setUp() throws Exception {
        containerPort = NetworkPortAssigner.retrieveNextPort();
        super.setUp();

        CLICK2DIAL_URL = "http://" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + httpContainerPort + "/click2call/call";

        receiverProtocolObjects = new ProtocolObjects(
                "receiver", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, true);
        SipProvider receiverProvider = receiver.createProvider();
        receiverProvider.addSipListener(receiver);

        CLICK2DIAL_PARAMS = "?from=sip:sipAppTest@" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5056&to=sip:to@" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + receiverPort;
        
        
        receiverProtocolObjects.start();

        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        deployApplication(params);
    }

    @Override
    public void tearDown() throws Exception {
        receiverProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }

    @Override
    public void deployApplication() {
    }
    
    public void deployApplication(Map<String, String> params) {
        SipStandardContext context = deployApplication(projectHome
                + "/sip-servlets-test-suite/applications/click-to-call-servlet/src/main/sipapp",
                "click2call",
                params, 
                null);
        assertTrue(context.getAvailable());
    }      

    @Override
    protected String getDarConfigurationFile() {
        return "file:///"
                + projectHome
                + "/sip-servlets-test-suite/testsuite/src/test/resources/"
                + "org/mobicents/servlet/sip/testsuite/click2call/click-to-call-dar.properties";
    }

    /**
     * Test that the sip app session is not invalidated and destroyed when only
     * the sip session is invalidated and not the http session
     *
     * @throws Exception
     */
    public void testSipAppSessionTerminationHttpSessionStillAlive()
            throws Exception {

        logger.info("Trying to reach url : " + CLICK2DIAL_URL
                + CLICK2DIAL_PARAMS);

        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();

        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) {
            httpResponse += (char) buffer[q];
        }
        logger.info("Received the follwing HTTP response: " + httpResponse);

        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
        Thread.sleep(TIMEOUT);
        Iterator<String> allMessagesIterator = receiver.getAllMessagesContent().iterator();
        logger.info("all messages received : ");
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertFalse(receiver.getAllMessagesContent().contains("sipAppSessionDestroyed"));
    }

    /**
     * Test if the sip app session is invalidated and destroyed when both the
     * sip session and http session are invalidated
     *
     * @throws Exception
     */
    public void testSipAppSessionTerminationHttpSessionInvalidated() throws Exception {

        logger.info("Trying to reach url : " + CLICK2DIAL_URL
                + CLICK2DIAL_PARAMS + "&invalidateHttpSession=true");

        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS + "&invalidateHttpSession=true");
        InputStream in = url.openConnection().getInputStream();

        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) {
            httpResponse += (char) buffer[q];
        }
        logger.info("Received the follwing HTTP response: " + httpResponse);

        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
        Thread.sleep(TIMEOUT);
        Iterator<String> allMessagesIterator = receiver.getAllMessagesContent().iterator();
        logger.info("all messages received : ");
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(receiver.getAllMessagesContent().contains("sipAppSessionDestroyed"));
    }
}
