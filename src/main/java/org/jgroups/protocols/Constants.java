package org.jgroups.protocols;

import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.pbcast.NAKACK2;

/**
 * Global IDs for manet protocols
 * @author Bela Ban
 * @since  0.0.1
 */
public class Constants {
    public static short NAKACK2_ID=ClassConfigurator.getProtocolId(NAKACK2.class);
    public static final short UNICAST_ID=ClassConfigurator.getProtocolId(UNICAST3.class);

    public static final short OMOLSR_ID=5580;
    public static final short OLSR_ID=5581;
    public static final short BW_CALC_ID=5582;
    public static final short SMCAST_ID=5583;
    public static final short JOLSR_UNICAST_ID=5584;
    public static final short JOLSR_UDP_ID=5585;
    public static final short FC_ID=5586;
    public static final short EMU_UDP_ID=5587;


    static {
        ClassConfigurator.addProtocol(OMOLSR_ID, OMOLSR.class);
        ClassConfigurator.addProtocol(OLSR_ID, OLSR.class);
        ClassConfigurator.addProtocol(BW_CALC_ID, BW_CALC.class);
        ClassConfigurator.addProtocol(SMCAST_ID, SMCAST.class);
        ClassConfigurator.addProtocol(JOLSR_UNICAST_ID, JOLSR_UNICAST.class);
        ClassConfigurator.addProtocol(JOLSR_UDP_ID, JOLSR_UDP.class);
        ClassConfigurator.addProtocol(FC_ID, FC.class);
        ClassConfigurator.addProtocol(EMU_UDP_ID, EMU_UDP.class);
    }
}
