package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import util.Tuple;

//import java.io.BufferedReader;
//import java.io.File;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;



public class HyperCubeRouter extends ActiveRouter {


    /** Hypercube router's setting namespace ({@value})*/
    public static final String HYPERCUBE_NS = "HyperCubeRouter";

//    /**
//     * Whether or not to generate random weights -setting id ({@value}).
//     * */
//    public static final String RANDOM_WEIGHTS ="randomWeights";

    /**
     * Namespace for given weight array -setting id ({@value}).
     */
    public static final String WEIGHT_ARRAY= "weightArray";
    /**
     * Namespace for given threshold -setting id ({@value}).
     */
    public static final String THRESHOLD= "threshold";

    /**
     * Namespace for the dimension of the hypercube -setting d ({@value}).
     */
    public static final String DIMENSION = "dimension";

//    /** HyperCube Routers rng seed -setting id ({@value})*/
//    public static final String RNG_SEED = "rngSeed";

    private int dimension;

    /**
     * Namespace for the filename of the individual profiles -setting id ({@value}).
     */
    public static final String PROFILE_FILENAME ="profileFilename";

    private String filename;


    /** common rng for all movement models in the simulation */
    protected static Random rng;

//    /** are the weights generated at random? */
//    private final boolean isRandom;

    /** weight vector used in threshold function */
    private ArrayList<Double> weightArray = new ArrayList<Double>();

    private float threshold;

    private String name;


    /** which corner in the hypercube does this host lie on? */
    private String corner;

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public HyperCubeRouter(Settings s) {
        super(s);
        Settings hypercubeSettings = new Settings(HYPERCUBE_NS);
//        this.isRandom = hypercubeSettings.getBoolean(RANDOM_WEIGHTS);
        this.dimension = hypercubeSettings.getInt(DIMENSION);
        this.filename = hypercubeSettings.getSetting(PROFILE_FILENAME);
        this.threshold = (float) hypercubeSettings.getDouble(THRESHOLD);
//        if (this.isRandom){
//                int seed = hypercubeSettings.getInt(RNG_SEED);
//                rng = new Random(seed);
//            randomWeights();
//        } else {
            for(Double item:hypercubeSettings.getCsvDoubles(WEIGHT_ARRAY)){
                this.weightArray.add(item);
            }
            System.out.print(Arrays.toString(weightArray.toArray()));

//        }
//        initProfile();

//        DTNHost host = getHost();
//        if (host != null){
//            System.out.print(host.getName());
//        }


        //TODO: add and define a function for init the weights
        //TODO: define a function for the threshold
    }
    public void setName(String name){
        this.name = name;
    }

//    public void randomWeights(){
//        for (int i=0;i<dimension;i++){
//            this.weightArray.add(rng.nextInt(100)/100.0);
//        }
//        System.out.print(Arrays.toString(weightArray.toArray()));
//    }

    public void initProfile() {
        File file = new File(this.filename);
        String id;
        String profile;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                id = scanner.next();
                profile = scanner.next();
//                System.out.println(id);
//                System.out.println(profile);
//                System.out.println(this.name);
                if (id.equals(this.name)){
                    this.corner = profile;
                    System.out.println(this.name);
                    System.out.println(this.corner);
                    return;
                }

            }
            scanner.close();
        }
        catch (FileNotFoundException e){
            System.out.print("File not found");
        }
    }
    public String getCorner(){
        return this.corner;
    }

    public Integer getDistance(String corner,String othCorner){
        int count = 0;
        for (int i = 0; i<corner.length();i++){
            if (corner.charAt(i)!=othCorner.charAt(i)){
                count++;
            }

        }
        return count;
    }
    public ArrayList<Double> getWeightArray(){
        return weightArray;
    }


    /**
     * Copyconstructor.
     * @param r The router prototype where setting values are copied from
     */
    protected HyperCubeRouter(HyperCubeRouter r) {
        super(r);
        this.filename = r.filename;
        this.weightArray = r.weightArray;


    }

    boolean thresholdFunction(HyperCubeRouter r,Message m){
        double sTor = 1.0/getDistance(r.getCorner(),this.getCorner());
        double rTod = 1.0/getDistance(r.getCorner(),((HyperCubeRouter) m.getTo().getRouter()).getCorner());
        double ihop = 1.0/m.getHops().size();
        double sum = 0;
        double[] factors = {sTor,rTod,ihop};
        ArrayList<Double> weights = getWeightArray();
        for (int i =0;i<weights.size();i++){
            sum += weights.get(i) *factors[i];
        }
//        return sigmoid(sum)>threshold;
        return true;
    }


    public static double sigmoid(double x) {
        return (1/( 1 + Math.pow(Math.E,(-1*x))));
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
                else if (thresholdFunction(othRouter,m)){
                    messages.add(new Tuple<Message, Connection>(m,con));
                }

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
