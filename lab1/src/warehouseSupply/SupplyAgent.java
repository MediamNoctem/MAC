package warehouseSupply;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;

// Агент по снабжению
public class SupplyAgent extends Agent {
    String[] clientRequests;
    boolean hasPurchaseFromSupplierCompleted = false;
    boolean hasBehaviorPerformed = false;
    String listOfPurchasedProducts = "";
    protected void setup() {
        System.out.println("\n------------------------------------------------------------\n");
        System.out.println("Агент снабжения склада " + getAID().getName() + " готов.\n");
        System.out.println("------------------------------------------------------------\n");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("supply-of-products");
        sd.setName("JADE-supply-agent");
        dfd.addServices(sd);
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
        System.out.println("Агент снабжения склада " + getAID().getName() + " завершил свою работу.");
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                clientRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();

                addBehaviour(new RequestPerformer());

                if (!hasBehaviorPerformed) {
                    block();
                }

                if (!hasPurchaseFromSupplierCompleted) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                    System.out.println("Требуемых товаров нет в наличии ни у одного из агентов-поставщиков.");
                }
                else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(listOfPurchasedProducts);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class RequestPerformer extends Behaviour {
        private AID supplier;
        private AID[] supplierAgents;
        private final ArrayList<AID> supplierAgentsPropose = new ArrayList<>();
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            for (int i = 0; i < clientRequests.length / 2; i++) {
                switch (step) {
                    case 0:
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sdt = new ServiceDescription();
                        sdt.setType("supply-" + clientRequests[2 * i + 1] + "-" + clientRequests[2 * i]);
                        template.addServices(sdt);
                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            if (result.length > 0) {
                                System.out.println("Найдены следующие агенты-поставщики:");
                                supplierAgents = new AID[result.length];
                                for (int j = 0; j < result.length; j++) {
                                    supplierAgents[j] = result[j].getName();
                                    System.out.println("    * " + supplierAgents[j].getName());
                                }
                                step = 1;
                            }
                            else {
                                System.out.println("Агенты-поставщики для товара " + clientRequests[2 * i] + ";" + clientRequests[2 * i + 1] + " не найдены.");
                            }
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                        break;
                    case 1:
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        for (int j = 0; j < supplierAgents.length; j++) {
                            cfp.addReceiver(supplierAgents[j]);
                        }
                        cfp.setContent(clientRequests[i * 2] + ";" + clientRequests[i * 2 + 1]);
                        cfp.setConversationId("supply-" + clientRequests[i * 2 + 1] + "-" + clientRequests[i * 2]);
                        cfp.setReplyWith("cfp" + System.currentTimeMillis());

                        myAgent.send(cfp);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("supply-" +
                                        clientRequests[i * 2 + 1] + "-" + clientRequests[i * 2]),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                        step = 2;
                        break;
                    case 2:
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                supplierAgentsPropose.add(reply.getSender());
                            }
                            repliesCnt++;
                            if (repliesCnt >= supplierAgents.length) {
                                Random random = new Random();
                                supplier = supplierAgentsPropose.get(random.nextInt(supplierAgentsPropose.size()));
                                step = 3;
                            }
                        } else {
                            block();
                        }
                        break;
                    case 3:
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(supplier);
                        order.setContent(clientRequests[i * 2] + ";" + clientRequests[i * 2 + 1]);
                        order.setConversationId("supply-" + clientRequests[i * 2 + 1] + "-" + clientRequests[i * 2]);
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);

                        mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId("supply-" + clientRequests[i * 2 + 1] + "-" +
                                        clientRequests[i * 2]), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        step = 4;
                        break;
                    case 4:
                        reply = myAgent.receive(mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                System.out.println("Товар " + clientRequests[i * 2] + ";" + clientRequests[i * 2 + 1] + " успешно приобретен через агента снабжения " +
                                        reply.getSender().getName() + " для агента менеджера " + reply.getSender().getName());

                                hasPurchaseFromSupplierCompleted = true;

                                listOfPurchasedProducts += clientRequests[i * 2] + ";" + clientRequests[i * 2 + 1] + ";";
                            } else {
                                System.out.println("Попытка не удалась: запрошенных товаров нет.");
                            }
                            step = 5;
                        } else {
                            block();
                        }
                        break;
                }
            }
            hasBehaviorPerformed = true;
        }

        public boolean done() {
            if (step == 3 && supplier == null) {
                System.out.println("Ошибка: запрошенных товаров нет.");
            }
            return ((step == 3 && supplier == null) || step == 5);
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
//                clientRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();
                if (msg.getContent().compareTo(listOfPurchasedProducts) == 0) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("Товары " + msg.getContent() + " были приобретены через агента по снабжению " +
                            getAID().getName() + " для агента-менеджера " + msg.getSender().getName());
                }
                else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                    System.out.println("Агент по снабжению " + msg.getSender().getName() +
                            " не смог приобрести необходимые товары");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
}
