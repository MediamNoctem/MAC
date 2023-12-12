package warehouseSupply;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.ArrayList;
import java.util.HashSet;

// Агент-поставщик
public class SupplierAgent extends Agent {
    private ArrayList<ArrayList<String>> catalogue;
    private String[] category;
    protected void setup() {
        System.out.println("Агент-поставщик " + getAID().getName() + " готов.");

        catalogue = new ArrayList<>();
        HashSet<String> categoryHashSet = new HashSet<>();
        Object[] args = getArguments();

        System.out.println("-----------------База товаров поставщика-----------------");
        for (int i = 0; i < args.length / 2; i++) {
            if (args.length > 0) {
                ArrayList<String> record = new ArrayList<>();
                record.add((String) args[2 * i]);
                record.add((String) args[2 * i + 1]);
                catalogue.add(record);
                categoryHashSet.add((String) args[2 * i + 1]);
                System.out.println("Запись " + i + ":\n" + "название товара: " + record.get(0) + "; категория товара: "
                        + record.get(1));
            }
        }
        System.out.println("---------------------------------------------------------\n");

        category = categoryHashSet.toArray(new String[0]);

        for (int i = 0; i < catalogue.size(); i++) {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("rent-apartment");
            sd.setName("JADE-travel-agency");
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

        addBehaviour(new TravelAgencyAgent.OfferRequestsServer());
        addBehaviour(new TravelAgencyAgent.PurchaseOrdersServer());
    }

}
