/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.endpoints.DummyEndpoint;
import de.uniluebeck.itm.ncoap.endpoints.server.NotObservableTestWebservice;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;

import java.net.InetSocketAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
* Tests to verify the server functionality related to separate responses.
*
* @author Oliver Kleine, Stefan Hueske
*/
public class ClientReceivesSeparateResponseTest extends AbstractCoapCommunicationTest{

    private static String PATH_TO_SERVICE = "/path/to/service";
    private static String PAYLOAD = "some arbitrary payload...";
    private static CoapServerApplication server;
    private static NotObservableTestWebservice service;

    private static DummyEndpoint endpoint;
    private static CoapRequest request;

    private static long requestSentTime;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints.DummyEndpoint")
                .setLevel(Level.INFO);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication();
        service = new NotObservableTestWebservice(PATH_TO_SERVICE, PAYLOAD, 0, 3000, server.getExecutor());
        server.registerService(service);

        endpoint = new DummyEndpoint();
        URI targetUri = new URI("coap://localhost:" + server.getPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        request.setMessageID(12345);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception{

//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET----------------->|        send GET-Request to server
//                  |                             |
//              (2) |<-------EMPTY-ACK------------|        server responds with empty ACK (after ~ 2 sec.)
//                  |                             |
//              (3) |<-------CON-RESPONSE---------|        server sends separate response (after ~ 3 sec.)
//                  |                             |
//              (4) |--------EMPTY-ACK----------->|        client confirms arrival
//                  |                             |


        //send request to testServer
        endpoint.writeMessage(request, new InetSocketAddress("localhost", server.getPort()));
        requestSentTime = System.currentTimeMillis();

        //wait for responses from server (one empty ACK and one CON response afterwards)
        Thread.sleep(3400);

        //let testEndpoint write empty ACK to acknowledge seperate response
        int messageID = endpoint.getReceivedCoapMessages()
                                .get(endpoint.getReceivedCoapMessages().lastKey())
                                .getMessageID();

        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        endpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", server.getPort()));

        //Wait some time to let the server receive the ACK and ensure there is no retransmission
        Thread.sleep(3000);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Receiver did not receive two messages";
        assertEquals(message, 2, endpoint.getReceivedCoapMessages().values().size());
    }

    @Test
    public void testReceiverReceivedEmptyAck() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();

        long emptyAckReceptionTime = receivedMessages.firstKey();
        long delay = requestSentTime - emptyAckReceptionTime;

        assertTrue("Empty ACK was too late (delay: " + delay + "ms.", delay < 2000);

        CoapMessage receivedMessage = receivedMessages.get(emptyAckReceptionTime);
        String message = "First received message is not an EMPTY ACK";

        assertEquals(message, MessageType.Name.ACK, receivedMessage.getMessageTypeName());
        assertEquals(message, MessageCode.Name.EMPTY, receivedMessage.getMessageCodeName());
    }

    @Test
    public void test2ndReceivedMessageIsResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());

        String message = "Endpoint received more than one message";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

    @Test
    public void test2ndReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response token does not match with request token";
        assertEquals(message, request.getToken(), receivedMessage.getToken());
    }

    @Test
    public void test2ndReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, MessageCode.Name.CONTENT_205, receivedMessage.getMessageCodeName());
    }

    @Test
    public void test2ndReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, PAYLOAD, receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void test2ndReceivedMessageHasMsgTypeCON() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response Msg EventType is not CON";
        assertEquals(message, MessageType.Name.CON, receivedMessage.getMessageTypeName());
    }
}
