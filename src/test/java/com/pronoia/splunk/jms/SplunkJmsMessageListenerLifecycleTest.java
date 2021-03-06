/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pronoia.splunk.jms;

import com.pronoia.junit.activemq.EmbeddedActiveMQBroker;
import com.pronoia.stub.httpec.EventCollectorClientStub;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Lifecycle tests for the SplunkJmsMessageListener class.
 */
public class SplunkJmsMessageListenerLifecycleTest {
    static final String DESTINATION_NAME = "audit.in";

    Logger log = LoggerFactory.getLogger(this.getClass());

    EventCollectorClientStub clientStub = new EventCollectorClientStub();

    EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker();

    SplunkJmsMessageListener instance;

    @Before
    public void setUp() throws Exception {
        String brokerURL = String.format("vm://%s?create=false&waitForStart=5000", broker.getBrokerName());

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(brokerURL);
        connectionFactory.setUserName("admin");
        connectionFactory.setPassword("admin");

        instance = new SplunkJmsMessageListener(DESTINATION_NAME);
        instance.setConnectionFactory(connectionFactory);

        log.info("Starting message listener");
        instance.setSplunkClient(clientStub);
    }

    @After
    public void tearDown() throws Exception {
        log.info("Stopping message listener");
        instance.stop();
    }

    /**
     * Make sure we get an exception if a connection cannot be established.
     *
     * @throws Exception in the event of a test error.
     */
    @Test(expected = IllegalStateException.class)
    public void testInitialConnectionFailure() throws Exception {
        instance.start();
    }

    /**
     * Make the listener stop when the connection is lost.
     *
     * @throws Exception in the event of a test error.
     */
    @Test
    public void testConnectionLost() throws Exception {
        broker.start();
        instance.start();

        assertTrue("Listener should be connectionStarted", instance.isConnectionStarted());

        broker.sendTextMessage(DESTINATION_NAME, "Dummy Body");

        Thread.sleep(1000);  // Wait for the listener to consume the message

        assertNotNull(clientStub.lastEvent);

        broker.stop();

        Thread.sleep(100);

        assertFalse("Listener should not be connectionStarted", instance.isConnectionStarted());
    }
}