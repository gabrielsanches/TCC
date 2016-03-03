
import java.io.IOException;
import java.text.DecimalFormat;
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
import org.cloudbus.cloudsim.UtilizationModelFull;
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
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerIbmX3250XeonX3470;
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
public class Main {

    /**
     * Creates main() to run this example.
     *
     * @param args the args
     * @throws IOException
     */
    //Alguma definicições da simulação como o intervalo, o limite da execução e se a saída em log está habilitad
    public final static double SCHEDULING_INTERVAL = 300;
    public final static double SIMULATION_LIMIT = 24 * 60 * 60;
    public final static boolean ENABLE_OUTPUT = true;
    
    //Configurações de Datacenter
    public final static int NUMBER_OF_HOSTS = 2000;
    public final static int NUMBER_0F_VMS = 2500;
    public final static String arch = "x86"; // system architecture
    public final static String os = "Linux"; // operating system
    public final static String vmm = "Xen";
    public final static double time_zone = 10.0; // time zone this resource located
    public final static double cost = 3.0; // the cost of using processing in this resource
    public final static double costPerMem = 0.05; // the cost of using memory in this resource
    public final static double costPerStorage = 0.001; // the cost of using storage in this resource
    public final static double costPerBw = 0.0; // the cost of using bw in this resource

    //definição da configuração das VMs utilizadas na simulação.
    public final static int VM_TYPES = 3;
    //public final static int[] VM_MIPS = {1000};
    //public final static int[] VM_PES = {2};
    //public final static int[] VM_RAM = {2048};
    public final static int[] VM_MIPS = {600,300,200};
    public final static int[] VM_PES = {4,2,1};
    public final static int[] VM_RAM = {4096,2048,2048};
    public final static int VM_BW = 100000; // 100 Mbit/s
    public final static int VM_SIZE = 2500; // 2.5 GB

    //Definição da configuração dos hosts utilizados na simulação.
    public final static int HOST_TYPES = 2;
    //public final static int[] HOST_MIPS = {2000};
    //public final static int[] HOST_PES = {4};
    //public final static int[] HOST_RAM = {16384};
    public final static int[] HOST_MIPS = {2000,1000};
    public final static int[] HOST_PES = {4,2};
    public final static int[] HOST_RAM = {16384,16384};
    public final static int HOST_BW = 1000000; // 1 Gbit/s
    public final static int HOST_STORAGE = 1000000; // 1 TB

    public final static PowerModel[] HOST_POWER = {
        new PowerModelSpecPowerHpProLiantMl110G4Xeon3040(),
        new PowerModelSpecPowerHpProLiantMl110G4Xeon3040()
    };

    //Definição da configuração dos cloudlets utilizados na simulação.
    public final static int CLOUDLET_LENGTH = 2500 * (int) SIMULATION_LIMIT;
    public final static int CLOUDLET_PES = 1;
    public final static long CLOUDLET_UTILIZATION_SEED = 1;
    public final static long fileSize = 300;
    public final static long outputSize = 300;

    //Função responsável pela criação da lista de VMs
    public static List<Vm> createVmList(int brokerId, int vmsNumber) {
        List<Vm> vms = new ArrayList<Vm>();
        for (int i = 0; i < vmsNumber; i++) {
            int vmType = i % VM_TYPES;
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

    //Função responsável pela criação do Datacenter
    public static Datacenter createDatacenter(
            String name,
            Class<? extends Datacenter> datacenterClass,
            List<PowerHost> hostList,
            VmAllocationPolicy vmAllocationPolicy) throws Exception {

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

    //Função responsável pela criação do DataBroker
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

    //Função responsável pela criação da lista de hosts
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

    //Função responsável pela criação da lista de cloudlets utilizados.
    public static List<Cloudlet> createCloudletList(int brokerId, int cloudletsNumber) {
        List<Cloudlet> list = new ArrayList<Cloudlet>();
        long seed = CLOUDLET_UTILIZATION_SEED;
        UtilizationModel utilizationModelNull = new UtilizationModelNull();
        UtilizationModel utilizationModelFull = new UtilizationModelFull();

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
                        utilizationModelFull,
                        utilizationModelFull);
            } else {
                cloudlet = new Cloudlet(
                        i,
                        CLOUDLET_LENGTH,
                        CLOUDLET_PES,
                        fileSize,
                        outputSize,
                        new UtilizationModelStochastic(seed * i),
                        utilizationModelFull,
                        utilizationModelFull);
            }
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(i);
            list.add(cloudlet);
        }

        return list;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Resource ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId());

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.printLine(indent + "SUCCESS"
                        + indent + indent + cloudlet.getResourceId()
                        + indent + cloudlet.getVmId()
                        + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent + dft.format(cloudlet.getFinishTime())
                );
            }
        }
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
                    NUMBER_0F_VMS);
            List<Vm> vmList = createVmList(brokerId, cloudletList.size());
            List<PowerHost> hostList = createHostList(NUMBER_OF_HOSTS);
            PowerDatacenter datacenter = (PowerDatacenter) createDatacenter(
                    "Datacenter",
                    PowerDatacenter.class,
                    hostList,
                    new WorstFitCpuAllocationPolicy(hostList));
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

            printCloudletList(newList);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }

        Log.printLine("Finished " + experimentName);
    }

}
