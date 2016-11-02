package org.openlcb;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.mockito.ArgumentMatcher;
import org.openlcb.can.AliasMap;
import org.openlcb.can.CanFrame;
import org.openlcb.can.GridConnect;
import org.openlcb.can.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.mockito.Mockito.*;

/**
 * Test helper class that instantiates an OlcbInterface and allows making expectations on what is
 * sent to the bus, as well as allows injecting response messages from the bus.
 *
 * Created by bracz on 1/9/16.
 */
public abstract class InterfaceTestBase extends TestCase {
    protected Connection outputConnectionMock = mock(AbstractConnection.class);
    protected OlcbInterface iface = null;
    protected AliasMap aliasMap = new AliasMap();
    protected boolean testWithCanFrameRendering = false;
    private boolean debugFrames = false;

    public InterfaceTestBase(String s) {
        super(s);
        expectInit();
    }

    public InterfaceTestBase() {
        expectInit();
    }

    private void expectInit() {
        NodeID id = new NodeID(new byte[]{1,2,0,0,1,1});
        aliasMap.insert(0x333, id);
        doCallRealMethod().when(outputConnectionMock).registerStartNotification(any());
        iface = new OlcbInterface(id, outputConnectionMock);
        verify(outputConnectionMock, atLeastOnce()).registerStartNotification(any());
        expectMessage(new InitializationCompleteMessage(iface.getNodeId()));
    }

    @Override
    protected void tearDown() throws Exception {
        expectNoMessages();
        super.tearDown();
    }

    /** Sends one or more OpenLCB message, as represented by the given CAN frames, to the
     * interface's inbound port. This represents traffic that a far away node is sending. The
     * frame should be specified in the GridConnect protocol.
     * @param frames is one or more CAN frames in the GridConnect protocol format.
     *  */
    protected void sendFrame(String frames) {
        List<CanFrame> parsedFrames = GridConnect.parse(frames);
        MessageBuilder d = new MessageBuilder(aliasMap);
        for (CanFrame f : parsedFrames) {
            List<Message> l = d.processFrame(f);
            if (l != null) {
                for (Message m : l) {
                    iface.getInputConnection().put(m, null);
                }
            }
        }
    }

    /** Sends an OpenLCB message to the interface's inbound port. This represents traffic that a
     * far away node is sending.
     * @param msg inbound message from a far node
     */
    protected void sendMessage(Message msg) {
        if (testWithCanFrameRendering) {
            MessageBuilder d = new MessageBuilder(aliasMap);
            List<? extends CanFrame> actualFrames = d.processMessage(msg);
            StringBuilder b = new StringBuilder();
            for (CanFrame f : actualFrames) {
                b.append(GridConnect.format(f));
            }
            if (debugFrames)System.err.println("Input frames: " + b);
            sendFrame(b.toString());
        } else {
            iface.getInputConnection().put(msg, null);
        }
    }

    /** Moves all outgoing messages to the pending messages queue. */
    protected void consumeMessages() {
        iface.flushSendQueue();
    }

    /** Expects that the next outgoing message (not yet matched with an expectation) is the given
     * CAN frame.
     * @param expectedFrame GridConnect-formatted CAN frame.
     */
    protected void expectFrame(final String expectedFrame) {
        class MessageMatchesFrame implements ArgumentMatcher<Message> {
            public boolean matches(Message message) {
                MessageBuilder d = new MessageBuilder(aliasMap);
                List<? extends CanFrame> actualFrames = d.processMessage(message);
                StringBuilder b = new StringBuilder();
                for (CanFrame f : actualFrames) {
                    b.append(GridConnect.format(f));
                }
                return expectedFrame == b.toString();
            }
            public String toString() {
                //printed in verification errors
                return "[OpenLCB message with CAN rendering of " + expectedFrame + "]";
            }
        }

        consumeMessages();
        verify(outputConnectionMock).put(argThat(new MessageMatchesFrame()),any());
    }

    /** Expects that the next outgoing message (not yet matched with an expectation) is the given
     * message.
     * @param expectedMessage message that should have been sent to the bus from the local stack.
     */
    protected void expectMessage(Message expectedMessage) {
        consumeMessages();
        verify(outputConnectionMock).put(eq(expectedMessage), any());
    }

    protected void expectMessageAndNoMore(Message expectedMessage) {
        expectMessage(expectedMessage);
        expectNoMessages();
    }

    /** Expects that there are no unconsumed outgoing messages. */
    protected void expectNoFrames() {
        consumeMessages();
        verifyNoMoreInteractions(outputConnectionMock);
    }

    /** Expects that there are no unconsumed outgoing messages. */
    protected void expectNoMessages() {
        expectNoFrames();
    }

    protected void sendFrameAndExpectResult(String send, String expect) {
        sendFrame(send);
        expectFrame(expect);
        expectNoFrames();
    }

    protected void sendMessageAndExpectResult(Message send, Message expect) {
        sendMessage(send);
        expectMessage(expect);
        clearInvocations(outputConnectionMock);
    }
}
