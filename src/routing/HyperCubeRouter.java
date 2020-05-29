package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import util.Tuple;

import java.util.*;



public class HyperCubeRouter extends ActiveRouter {


    /** Prophet router's setting namespace ({@value})*/
    public static final String HYPERCUBE_NS = "HyperCubeRouter";

    /**
     * Whether or not to generate random weights -setting id ({@value}).
     * */
    public static final String RANDOM_WEIGHTS ="randomWeights";

    /**
     * Namespace for given weight array -setting id ({@value}).
     */
    public static final String WEIGHT_ARRAY= "weightArray";

    /** common rng for generating random profiles in the simulation */
    protected static Random rng;

    /** are the weights generated at random? */
    private final boolean isRandom;

    /** weight vector used in threshold function */
    private double[] weightArray = {};


    /** which corner in the hypercube does this host lie on? */
    private Map<DTNHost, String> corner;

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public HyperCubeRouter(Settings s) {
        super(s);
        Settings hypercubeSettings = new Settings(HYPERCUBE_NS);
        isRandom = hypercubeSettings.getBoolean(RANDOM_WEIGHTS);
        if (isRandom){
            System.out.print("The random weight function needs to be defined");
//            weightArray = randomweights()
        } else {
            weightArray = hypercubeSettings.getCsvDoubles(WEIGHT_ARRAY);
            System.out.print(Arrays.toString(weightArray));

        }

//        DTNHost host = getHost();
//        if (host != null){
//            System.out.print(host.getName());
//        }


        //TODO: add and define a function for init the weights
        //TODO: define a function for the threshold
    }

//    private void initWeights(Settings s){
//        s.getSetting();
//    }

    /**
     * Copyconstructor.
     * @param r The router prototype where setting values are copied from
     */
    protected HyperCubeRouter(HyperCubeRouter r) {
        super(r);
        this.isRandom = r.isRandom;


    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() ||isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        tryOtherMessages();
    }

    /**
     * Tries to send all other messages to all connected hosts ordered by
     * their delivery probability
     * @return The return value of {@link #tryMessagesForConnected(List)}
     */
    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            HyperCubeRouter othRouter = (HyperCubeRouter)other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                //TODO: Change this condition to use the threshold function
//                if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
//                    // the other node has higher probability of delivery
//                    messages.add(new Tuple<Message, Connection>(m,con));
//                rng.d;
//                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }

        // sort the message-connection tuples
//        Collections.sort(messages, new HyperCubeRouter.TupleComparator());
        return tryMessagesForConnected(messages);	// try to send messages
    }


    @Override
    public MessageRouter replicate() {
        HyperCubeRouter r = new HyperCubeRouter(this);
        return r;
    }
}
