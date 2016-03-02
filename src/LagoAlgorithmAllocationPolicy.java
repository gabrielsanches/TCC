
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyAbstract;

/**
 * Created by Gabriel on 08/01/2016.
 */
public class LagoAlgorithmAllocationPolicy extends PowerVmAllocationPolicyAbstract {

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

    public LagoAlgorithmAllocationPolicy(List<? extends Host> list) {
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

        double bestEfficiency = Integer.MIN_VALUE;
        PowerHost bestHost = null;
        for (PowerHost host : this.<PowerHost>getHostList()) {
            double utilization = vm.getMips() + host.getUtilizationMips();
            if (utilization < host.getTotalMips()) {
                double efficiency = host.getTotalMips() / host.getMaxPower();
                if (efficiency > bestEfficiency) {
                    bestEfficiency = efficiency;
                    bestHost = host;
                } else {
                    if (efficiency == bestEfficiency) {
                        double pw_vm_at_host = bestHost.getPower()
                                + getPowerAfterAllocation(host, vm);
                        double pw_vm_at_bestHost = host.getPower()
                                + getPowerAfterAllocation(bestHost, vm);
                        if (pw_vm_at_host < pw_vm_at_bestHost) {
                            bestHost = host;
                        } else {
                            if (pw_vm_at_host == pw_vm_at_bestHost) {
                                if (host.getUtilizationOfCpu() > bestHost.getUtilizationOfCpu()) {
                                    bestHost = host;
                                } else {
                                    if (host.getUtilizationOfCpu() == bestHost.getUtilizationOfCpu()) {
                                        if (host.getTotalMips() > bestHost.getTotalMips()) {
                                            bestHost = host;
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        return bestHost;
    }

    public double getPowerAfterAllocation(PowerHost host, Vm vm) {
        double power = 0;
        try {
            //System.out.println(getMaxUtilizationAfterAllocation(host, vm));
            power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    public double getMaxUtilizationAfterAllocation(PowerHost host, Vm vm) {
        double requestedTotalMips = vm.getCurrentRequestedTotalMips();
        double hostUtilizationMips = getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    public double getUtilizationOfCpuMips(PowerHost host) {
        double hostUtilizationMips = 0;
        for (Vm vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
        }
        return hostUtilizationMips;
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

    /**
     * Allocates a host for a given VM.
     *
     * @param vm VM specification
     * @return $true if the host could be allocated; $false otherwise
     * @pre $none
     * @post $none
     */
    /*
     @Override
     public boolean allocateHostForVm(Vm vm) {
     int requiredPes = vm.getNumberOfPes();
     boolean result = false;
     int tries = 0;
     List<Integer> freePesTmp = new ArrayList<Integer>();
     for (Integer freePes : getFreePes()) {
     freePesTmp.add(freePes);
     }

     if (!getVmTable().containsKey(vm.getUid())) { // if this vm was not created
     do {// we still trying until we find a host or until we try all of them
     int moreFree = Integer.MIN_VALUE;
     int idx = -1;

     // we want the host with less pes in use
     for (int i = 0; i < freePesTmp.size(); i++) {
     if (freePesTmp.get(i) > moreFree) {
     moreFree = freePesTmp.get(i);
     idx = i;
     }
     }

     Host host = getHostList().get(idx);
     result = host.vmCreate(vm);

     if (result) { // if vm were succesfully created in the host
     getVmTable().put(vm.getUid(), host);
     getUsedPes().put(vm.getUid(), requiredPes);
     getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
     result = true;
     break;
     } else {
     freePesTmp.set(idx, Integer.MIN_VALUE);
     }
     tries++;
     } while (!result && tries < getFreePes().size());

     }

     return result;
     }*/
}
