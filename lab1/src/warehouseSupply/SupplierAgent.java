package warehouseSupply;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for (ArrayList<String> record : catalogue) {
            ServiceDescription sd = new ServiceDescription();
            sd.setType("supply-" + record.get(1) + "-" + record.get(0));
            sd.setName("JADE-supplier-agent");
            dfd.addServices(sd);
        }
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Агент-поставщик " + getAID().getName() + " завершил свою работу.");
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String[] clientRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();

                ArrayList<String> tmp = new ArrayList<>();
                tmp.add(clientRequests[0]);
                tmp.add(clientRequests[1]);

                int index = catalogue.indexOf(tmp);

                if (index == -1) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                    System.out.println("Требуемого товара нет в наличии у агента-поставщика.");
                }
                else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(msg.getContent());
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String[] clientRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();

                ArrayList<String> tmp = new ArrayList<>();
                tmp.add(clientRequests[0]);
                tmp.add(clientRequests[1]);

                int index = catalogue.indexOf(tmp);

                if (index != -1) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("Товар " + msg.getContent() + " был продан агенту " + msg.getSender().getName());
                }
                else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                    System.out.println("Требуемого товара нет у агента-поставщика.");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
}
