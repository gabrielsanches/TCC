
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.examples.power.Helper;
import org.cloudbus.cloudsim.examples.power.random.RandomHelper;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.power.Constants;
import org.cloudbus.cloudsim.examples.power.random.RandomConstants;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.power.PowerDatacenterNonPowerAware;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G4Xeon3040;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * A simulation of a heterogeneous non-power aware data center: all hosts
 * consume maximum power all the time.
 *
 * The remaining configuration parameters are in the Constants and
 * RandomConstants classes.
 *
 * If you are using any algorithms, policies or workload included in the power
 * package please cite the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic
 * Algorithms and Adaptive Heuristics for Energy and Performance Efficient
 * Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency
 * and Computation: Practice and Experience (CCPE), Volume 24, Issue 13, Pages:
 * 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class teste {

    /**
     * Creates main() to run this example.
     *
     * @param args the args
     * @throws IOException
     */
    public final static double SCHEDULING_INTERVAL = 300;
    //public final static double SIMULATION_LIMIT = 24 * 60 * 60;
    public final static double SIMULATION_LIMIT = 1000;
    public final static boolean ENABLE_OUTPUT = true;

    public final static int NUMBER_OF_HOSTS = 50;

    //definição da configuração das VMs utilizadas na simulação.
    public final static int VM_TYPES = 4;
    public final static int[] VM_MIPS = {2500, 2000, 1000, 500};
    public final static int[] VM_PES = {1, 1, 1, 1};
    public final static int[] VM_RAM = {870, 1740, 1740, 613};
    public final static int VM_BW = 100000; // 100 Mbit/s
    public final static int VM_SIZE = 2500; // 2.5 GB

    //Definição da configuração dos hosts utilizados na simulação.
    public final static int HOST_TYPES = 2;
    public final static int[] HOST_MIPS = {1860, 2660};
    public final static int[] HOST_PES = {2, 2};
    public final static int[] HOST_RAM = {4096, 4096};
    public final static int HOST_BW = 1000000; // 1 Gbit/s
    public final static int HOST_STORAGE = 1000000; // 1 TB

    public final static PowerModel[] HOST_POWER = {
        new PowerModelSpecPowerHpProLiantMl110G4Xeon3040(),
        new PowerModelSpecPowerHpProLiantMl110G5Xeon3075()
    };

    //Definição da configuração dos cloudlets utilizados na simulação.
    public final static long CLOUDLET_UTILIZATION_SEED = 1;
    public final static int CLOUDLET_LENGTH = 2500 * (int) SIMULATION_LIMIT;
    public final static int CLOUDLET_PES = 1;

    public static List<Vm> createVmList(int brokerId, int vmsNumber) {
        List<Vm> vms = new ArrayList<Vm>();
        for (int i = 0; i < vmsNumber; i++) {
            int vmType = i / (int) Math.ceil((double) vmsNumber / VM_TYPES);
            vms.add(new PowerVm(
                    i,
                    brokerId,
                    VM_MIPS[vmType],
                    VM_PES[vmType],
                    VM_RAM[vmType],
                    VM_BW,
                    VM_SIZE,
                    1,
                    "Xen",
                    new CloudletSchedulerDynamicWorkload(VM_MIPS[vmType], VM_PES[vmType]),
                    SCHEDULING_INTERVAL));
        }
        return vms;
    }

    public static Datacenter createDatacenter(
            String name,
            Class<? extends Datacenter> datacenterClass,
            List<PowerHost> hostList,
            VmAllocationPolicy vmAllocationPolicy) throws Exception {
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this resource
        double costPerBw = 0.0; // the cost of using bw in this resource

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch,
                os,
                vmm,
                hostList,
                time_zone,
                cost,
                costPerMem,
                costPerStorage,
                costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = datacenterClass.getConstructor(
                    String.class,
                    DatacenterCharacteristics.class,
                    VmAllocationPolicy.class,
                    List.class,
                    Double.TYPE).newInstance(
                            name,
                            characteristics,
                            vmAllocationPolicy,
                            new LinkedList<Storage>(),
                            SCHEDULING_INTERVAL);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        return datacenter;
    }

    public static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new PowerDatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return broker;
    }

    public static List<PowerHost> createHostList(int hostsNumber) {
        List<PowerHost> hostList = new ArrayList<PowerHost>();
        for (int i = 0; i < hostsNumber; i++) {
            int hostType = i % HOST_TYPES;

            List<Pe> peList = new ArrayList<Pe>();
            for (int j = 0; j < HOST_PES[hostType]; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(HOST_MIPS[hostType])));
            }

            hostList.add(new PowerHostUtilizationHistory(
                    i,
                    new RamProvisionerSimple(HOST_RAM[hostType]),
                    new BwProvisionerSimple(HOST_BW),
                    HOST_STORAGE,
                    peList,
                    new VmSchedulerTimeSharedOverSubscription(peList),
                    HOST_POWER[hostType]));
        }
        return hostList;
    }

    public static List<Cloudlet> createCloudletList(int brokerId, int cloudletsNumber) {
        List<Cloudlet> list = new ArrayList<Cloudlet>();

        long fileSize = 300;
        long outputSize = 300;
        long seed = CLOUDLET_UTILIZATION_SEED;
        UtilizationModel utilizationModelNull = new UtilizationModelNull();

        for (int i = 0; i < cloudletsNumber; i++) {
            Cloudlet cloudlet = null;
            if (seed == -1) {
                cloudlet = new Cloudlet(
                        i,
                        CLOUDLET_LENGTH,
                        CLOUDLET_PES,
                        fileSize,
                        outputSize,
                        new UtilizationModelStochastic(),
                        utilizationModelNull,
                        utilizationModelNull);
            } else {
                cloudlet = new Cloudlet(
                        i,
                        CLOUDLET_LENGTH,
                        CLOUDLET_PES,
                        fileSize,
                        outputSize,
                        new UtilizationModelStochastic(seed * i),
                        utilizationModelNull,
                        utilizationModelNull);
            }
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(i);
            list.add(cloudlet);
        }

        return list;
    }

    public static void main(String[] args) throws IOException {
        String experimentName = "Ambiente de teste para a alocação de máquinas virtuais";
        String outputFolder = "output";

        Log.setDisabled(!ENABLE_OUTPUT);
        Log.printLine("Starting " + experimentName);

        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            List<Cloudlet> cloudletList = createCloudletList(
                    brokerId,
                    100);
            List<Vm> vmList = createVmList(brokerId, cloudletList.size());
            List<PowerHost> hostList = createHostList(NUMBER_OF_HOSTS);
            PowerDatacenter datacenter = (PowerDatacenter) createDatacenter(
                    "Datacenter",
                    PowerDatacenter.class,
                    hostList,
                    new WorstFitRamAllocationPolicy(hostList));
            datacenter.setDisableMigrations(true);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(SIMULATION_LIMIT);
            double lastClock = CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");

            CloudSim.stopSimulation();

            Helper.printResults(
                    datacenter,
                    vmList,
                    lastClock,
                    experimentName,
                    false,
                    outputFolder);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }

        Log.printLine("Finished " + experimentName);
    }
    
}
