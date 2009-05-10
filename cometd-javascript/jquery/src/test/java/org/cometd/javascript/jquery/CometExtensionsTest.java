package org.cometd.javascript.jquery;

import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.Test;

/**
 * @version $Revision: 1453 $ $Date: 2009-02-25 12:57:20 +0100 (Wed, 25 Feb 2009) $
 */
public class CometExtensionsTest extends AbstractJQueryCometTest
{
    @Test
    public void testRegisterUnregister() throws Exception
    {
        evaluateScript("$.cometd.setLogLevel('debug');");
        evaluateScript("var inCount = 0;");
        evaluateScript("var outCount = 0;");
        evaluateScript("$.cometd.registerExtension('testin', {" +
                "incoming: function(message) { ++inCount; }" +
                "});");
        evaluateScript("$.cometd.registerExtension('testout', {" +
                "outgoing: function(message) { ++outCount; }" +
                "});");
        evaluateScript("$.cometd.registerExtension('testempty', {});");

        evaluateScript("$.cometd.init('" + cometURL + "')");
        Thread.sleep(500); // Wait for the long poll

        Number inCount = get("inCount");
        Number outCount = get("outCount");
        assert inCount.intValue() == 2; // handshake, connect1
        assert outCount.intValue() == 3; // handshake, connect1, connect2

        Boolean unregistered = evaluateScript("$.cometd.unregisterExtension('testin');");
        assert unregistered;
        unregistered = evaluateScript("$.cometd.unregisterExtension('testout');");
        assert unregistered;

        evaluateScript("$.cometd.publish('/echo', 'ping');");
        Thread.sleep(500); // Wait for the publish to return
        inCount = get("inCount");
        outCount = get("outCount");
        assert inCount.intValue() == 2;
        assert outCount.intValue() == 3;

        evaluateScript("$.cometd.disconnect();");
        Thread.sleep(500); // Wait for the disconnect to return
    }

    @Test
    public void testExtensions() throws Exception
    {
        evaluateScript("$.cometd.setLogLevel('debug');");
        defineClass(Listener.class);

        StringBuilder script = new StringBuilder();
        script.append("var listener = new Listener();");
        script.append("$.cometd.registerExtension('testext', {" +
                "incoming: function(message) { listener.incoming(message); }," +
                "outgoing: function(message) { listener.outgoing(message); }" +
                "});");
        evaluateScript(script.toString());
        Listener listener = get("listener");

        evaluateScript("$.cometd.init('" + cometURL + "')");
        Thread.sleep(500); // Wait for the long poll
        assert listener.getOutgoingMessageCount() == 3; // handshake, connect1, connect2
        assert listener.getIncomingMessageCount() == 2; // handshake, connect1

        listener.reset();
        script.setLength(0);
        script.append("var subscription = $.cometd.subscribe('/echo', window.console, window.console.debug);");
        evaluateScript(script.toString());
        Thread.sleep(500); // Wait for subscribe to happen
        assert listener.getOutgoingMessageCount() == 1; // subscribe
        assert listener.getIncomingMessageCount() == 1; // subscribe

        listener.reset();
        script.setLength(0);
        script.append("$.cometd.publish('/echo', 'test');");
        evaluateScript(script.toString());
        Thread.sleep(500); // Wait for subscribe to happen
        assert listener.getOutgoingMessageCount() == 1; // publish
        assert listener.getIncomingMessageCount() == 2; // publish, message

        listener.reset();
        script.setLength(0);
        script.append("$.cometd.unsubscribe(subscription);");
        evaluateScript(script.toString());
        Thread.sleep(500); // Wait for subscribe to happen
        assert listener.getOutgoingMessageCount() == 1; // unsubscribe
        assert listener.getIncomingMessageCount() == 1; // unsubscribe

        listener.reset();
        script.setLength(0);
        script.append("$.cometd.disconnect();");
        evaluateScript(script.toString());
        Thread.sleep(500); // Wait for disconnect to happen
        assert listener.getOutgoingMessageCount() == 1; // disconnect
        assert listener.getIncomingMessageCount() == 2; // connect2, disconnect
    }

    public static class Listener extends ScriptableObject
    {
        private int outgoing;
        private int incoming;

        public void jsFunction_outgoing(Object message)
        {
            ++outgoing;
        }

        public void jsFunction_incoming(Object message)
        {
            ++incoming;
        }

        public String getClassName()
        {
            return "Listener";
        }

        public int getOutgoingMessageCount()
        {
            return outgoing;
        }

        public int getIncomingMessageCount()
        {
            return incoming;
        }

        public void reset()
        {
            incoming = 0;
            outgoing = 0;
        }
    }
}