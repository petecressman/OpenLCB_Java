// CdiMemConfigReader.java

package org.openlcb.cdi.jdom;

import javax.swing.*;
import javax.swing.text.*;
import java.beans.PropertyChangeListener;

import org.openlcb.*;
import org.openlcb.Utilities;
import org.openlcb.implementations.*;

/**
 * Provide a Reader to the OpenLCB CDI in a node.
 *
 * This first implementation reads the entire data before providing the Reader
 * by call back.
 *
 * @author	Bob Jacobsen   Copyright (C) 2012
 * @version	$Revision$
 */
public class CdiMemConfigReader  {
    
    final static int LENGTH = 64;

    NodeID node;
    MimicNodeStore store;
    MemoryConfigurationService service;
    final int space;
        
    public CdiMemConfigReader(NodeID node, MimicNodeStore store, MemoryConfigurationService service) {
        this.node = node;
        this.store = store;
        this.service = service;
        this.space = MemoryConfigurationService.SPACE_CDI;
    }

    public CdiMemConfigReader(NodeID node, OlcbInterface iface, int space) {
        this.node = node;
        this.store = iface.getNodeStore();
        this.service = iface.getMemoryConfigurationService();
        this.space = space;
    }


    long nextAddress = 0;
    StringBuffer buf;
    
    ReaderAccess retval;
    public void startLoadReader(ReaderAccess retval) {
        this.retval = retval;
        nextAddress = 0;
        buf = new StringBuffer();
        nextRequest();
    }
    
    void nextRequest() {
        MemoryConfigurationService.McsReadMemo memo = 
            new MemoryConfigurationService.McsReadMemo(node, space, nextAddress, LENGTH) {
                public void handleReadData(NodeID dest, int space, long address, byte[] data) { 
                    // handle return data, checking for null in string or zero-length reply
                    if (data.length == 0) {
                        done();
                        return;  // don't do next request
                    }
                    for (int i = 0; i<data.length; i++) {
                        if (data[i] == 0) {
                            done();
                            return;  // don't do next request
                        }
                        buf.append((char)data[i]);
                    }
                    // repeat if not done
                    nextAddress = nextAddress + LENGTH;
                    nextRequest();
                }
            };
        service.request(memo);
    }
    
    private void done() {
        // done, pass back a reader based on the current buffer contents
        if (retval != null) 
            retval.provideReader(new java.io.StringReader(new String(buf)));
    }
    
    public interface ReaderAccess {
        public void provideReader(java.io.Reader r);
    }
}
