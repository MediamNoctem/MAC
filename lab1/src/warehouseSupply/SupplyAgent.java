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

// Агент по снабжению
public class SupplyAgent extends Agent {
    String[] clientRequests;
    protected void setup() {
        System.out.println("Агент снабжения склада " + getAID().getName() + " готов.\n");

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

                myAgent.addBehaviour(new RequestPerformer());

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

    private class RequestPerformer extends Behaviour {
        private AID supplier;
        private AID[] supplierAgents;
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
                            System.out.println("Найдены следующие агенты-поставщики:");
                            supplierAgents = new AID[result.length];
                            for (int j = 0; j < result.length; ++j) {
                                supplierAgents[j] = result[j].getName();
                                System.out.println("    * " + supplierAgents[j].getName());
                            }
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                        step = 1;
                        break;
                    case 1:
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        for (int j = 0; j < supplierAgents.length; ++j) {
                            cfp.addReceiver(supplierAgents[j]);
                        }
                        cfp.setContent("");
                        cfp.setConversationId("supply-of-products");
                        cfp.setReplyWith("cfp" + System.currentTimeMillis());

                        myAgent.send(cfp);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("supply-of-products"),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                        step = 1;
                        break;
                    case 2:
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                // String[] listOfFoundProducts = reply.getContent().split(";");
                                supplier = reply.getSender();
                            }
                            step = 2;
                        } else {
                            block();
                        }
                        break;
                    case 3:
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(supplier);
                        order.setContent("");
                        order.setConversationId("supply-of-products");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);

                        mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId("supply-of-products"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        step = 3;
                        break;
                    case 4:
                        reply = myAgent.receive(mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                System.out.println("Товары успешно приобретены через агента снабжения " + reply.getSender().getName());
                                myAgent.doDelete();
                            } else {
                                System.out.println("Попытка не удалась: запрошенных товаров нет.");
                            }
                            step = 4;
                        } else {
                            block();
                        }
                        break;
                }
            }
        }

        public boolean done() {
            if (step == 2 && supplier == null) {
                System.out.println("Ошибка: запрошенных товаров нет.");
            }
            return ((step == 2 && supplier == null) || step == 4);
        }
    }
}
