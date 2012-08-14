package org.buddycloud.channelserver.packetprocessor.iq.namespace.pubsub.get;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.buddycloud.channelserver.db.DataStore;
import org.buddycloud.channelserver.db.jedis.NodeSubscriptionImpl;
import org.buddycloud.channelserver.packetprocessor.iq.namespace.pubsub.JabberPubsub;
import org.buddycloud.channelserver.packetprocessor.iq.namespace.pubsub.PubSubElementProcessor;
import org.buddycloud.channelserver.packetprocessor.iq.namespace.pubsub.PubSubGet;
import org.buddycloud.channelserver.pubsub.subscription.NodeSubscription;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

public class SubscriptionsGet implements PubSubElementProcessor {

    private final BlockingQueue<Packet> outQueue;
    private final DataStore dataStore;
    
    public SubscriptionsGet(BlockingQueue<Packet> outQueue, DataStore dataStore) {
        this.outQueue = outQueue;
        this.dataStore = dataStore;
    }

    @Override
    public void process(Element elm, JID actorJID, IQ reqIQ, Element rsm) throws Exception {
        
        IQ result = IQ.createResultIQ(reqIQ);
        
        Element pubsub = result.setChildElement(PubSubGet.ELEMENT_NAME, elm.getNamespaceURI());
        Element subscriptions = pubsub.addElement("subscriptions");
        
        String node = elm.attributeValue("node");

        if(actorJID == null) {
            actorJID = reqIQ.getFrom();
        }
        
        if(node == null) {
            // let's get all subscriptions.
            
            Iterator<? extends NodeSubscription> cur = dataStore.getUserSubscriptionsOfNodes(actorJID.toBareJID());
            while(cur.hasNext()) {
                NodeSubscription ns = cur.next();
                subscriptions.addElement("subscription")
                             .addAttribute("node", ns.getNode())
                             .addAttribute("subscription", ns.getSubscription())
                             .addAttribute("jid", ns.getBareJID());
            }
            
        } else {
            // Let's get only one subscription.
            NodeSubscriptionImpl ns = dataStore.getUserSubscriptionOfNode(actorJID.toBareJID(), node);
            if(ns.getSubscription() != null) {
            
                subscriptions.addAttribute("node", node);
                subscriptions.addElement("subscription")
                             .addAttribute("node", ns.getNode())
                             .addAttribute("subscription", ns.getSubscription())
                             .addAttribute("jid", ns.getBareJID());
            }
        }
        
        outQueue.put(result);
        
    }

    @Override
    public boolean accept(Element elm) {
        return elm.getName().equals("subscriptions");
    }

}
