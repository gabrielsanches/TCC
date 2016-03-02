
import static java.lang.Math.random;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyAbstract;

/**
 * Created by Gabriel on 08/01/2016.
 */
public class RandomAllocationPolicy extends PowerVmAllocationPolicyAbstract {

    /**
     * Tabela de máquinas virtuais.
     */
    private Map<String, Host> vmTable;

    /**
     * Núcleos, Ram, processamento por segundo, banda, armazenamento utilizados.
     */
    private Map<String, Integer> usedPes;

    /**
     * Quantidade de núcleos de processador (Pes), quantidade de Ram, quantidade
     * de processamento por segundo, quantidade de banda, quantidade de
     * armazenamento livres para uso.
     */
    private List<Integer> freePes;

    public RandomAllocationPolicy(List<? extends Host> list) {
        super(list);

        setFreePes(new ArrayList<Integer>());
        for (Host host : getHostList()) {
            getFreePes().add(host.getNumberOfPes());
        }

        setVmTable(new HashMap<String, Host>());
        setUsedPes(new HashMap<String, Integer>());
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        return allocateHostForVm(vm, findHostForVm(vm));
    }

    public PowerHost findHostForVm(Vm vm) {
        Random random = new Random();

        while(true) {

            int rand = random.nextInt(getHostList().size() - 1);
            PowerHost h = (PowerHost) getHostList().get(rand);
            if (h.isSuitableForVm(vm) && (getFreePes().get(h.getId()) > 0)) {
                return h;
            }
        }
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (host.vmCreate(vm)) { // if vm has been succesfully created in the host
            getVmTable().put(vm.getUid(), host);

            int requiredPes = vm.getNumberOfPes();
            int idx = getHostList().indexOf(host);
            getUsedPes().put(vm.getUid(), requiredPes);
            getFreePes().set(idx, getFreePes().get(idx) - requiredPes);

            Log.formatLine(
                    "%.2f: VM #" + vm.getId() + " has been allocated to the host #" + host.getId(),
                    CloudSim.clock());
            return true;
        }

        return false;
    }

    /**
     * Releases the host used by a VM.
     *
     * @param vm the vm
     * @pre $none
     * @post none
     */
    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = getVmTable().remove(vm.getUid());
        int idx = getHostList().indexOf(host);
        int pes = getUsedPes().remove(vm.getUid());
        if (host != null) {
            host.vmDestroy(vm);
            getFreePes().set(idx, getFreePes().get(idx) + pes);
        }
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Gets the host that is executing the given VM belonging to the given user.
     *
     * @param vm the vm
     * @return the Host with the given vmID and userID; $null if not found
     * @pre $none
     * @post $none
     */
    @Override
    public Host getHost(Vm vm) {
        return getVmTable().get(vm.getUid());
    }

    /**
     * Gets the host that is executing the given VM belonging to the given user.
     *
     * @param vmId the vm id
     * @param userId the user id
     * @return the Host with the given vmID and userID; $null if not found
     * @pre $none
     * @post $none
     */
    @Override
    public Host getHost(int vmId, int userId) {
        return getVmTable().get(Vm.getUid(userId, vmId));
    }

    /**
     * Gets the vm table.
     *
     * @return the vm table
     */
    public Map<String, Host> getVmTable() {
        return vmTable;
    }

    /**
     * Sets the vm table.
     *
     * @param vmTable the vm table
     */
    protected void setVmTable(Map<String, Host> vmTable) {
        this.vmTable = vmTable;
    }

    /**
     * Gets the used pes.
     *
     * @return the used pes
     */
    protected Map<String, Integer> getUsedPes() {
        return usedPes;
    }

    /**
     * Sets the used pes.
     *
     * @param usedPes the used pes
     */
    protected void setUsedPes(Map<String, Integer> usedPes) {
        this.usedPes = usedPes;
    }

    /**
     * Gets the free pes.
     *
     * @return the free pes
     */
    protected List<Integer> getFreePes() {
        return freePes;
    }

    /**
     * Sets the free pes.
     *
     * @param freePes the new free pes
     */
    protected void setFreePes(List<Integer> freePes) {
        this.freePes = freePes;
    }

}
