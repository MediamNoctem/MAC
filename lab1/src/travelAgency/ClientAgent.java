package travelAgency;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class ClientAgent extends Agent {
    private static final long serialVersionUID = 1L;
    public Record customerRequests;
    private AID[] sellerAgents;

    protected void setup() {
        System.out.println("Привет! Клиент " + getAID().getName() + " готов.");

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            customerRequests = new Record((String) args[0], (String) args[1],Integer.parseInt((String) args[2]),
                    Integer.parseInt((String) args[3]));
            System.out.println("Клиентские требования:\n" + customerRequests.String());

            addBehaviour(new TickerBehaviour(this, 30000) {
                private static final long serialVersionUID = 1L;

                protected void onTick() {
                    System.out.println("Пытаемся купить апартаменты, подходящие под условия:\n" + customerRequests.String());
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("rent-apartment");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Найдены следующие турагенства:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getName());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    myAgent.addBehaviour(new ClientAgent.RequestPerformer());
                }
            } );
        }
        else {
            System.out.println("Не указаны клиентские требования.");
            doDelete();
        }
    }

    protected void takeDown() {
        System.out.println("Клиент " + getAID().getName() + " завершил свою работу.");
    }

    private class RequestPerformer extends Behaviour {
        private static final long serialVersionUID = 1L;
        private AID bestSeller;
        private String country;
        private String bestApartmentClass;
        private int bestCost;
        private int durationRental;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(customerRequests.toString());
                    cfp.setConversationId("rent-apartment");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("rent-apartment"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String[] optionsFromTravelAgency = reply.getContent().split(";");
                            String curApartmentClass;
                            int curCost;

                            country = optionsFromTravelAgency[0];
                            bestApartmentClass = optionsFromTravelAgency[1];
                            bestCost = Integer.parseInt(optionsFromTravelAgency[2]);
                            durationRental = customerRequests.durationRental;
                            bestSeller = reply.getSender();

                            for (int i = 1; i < optionsFromTravelAgency.length / 4; i++) {
                                curApartmentClass = optionsFromTravelAgency[4 * i + 1];
                                curCost = Integer.parseInt(optionsFromTravelAgency[4 * i + 2]);
                                if (bestCost > curCost) {
                                    bestCost = curCost;
                                    bestApartmentClass = curApartmentClass;
                                }
                                else {
                                    if (bestCost == curCost) {
                                        if (bestApartmentClass.compareTo(curApartmentClass) < 0) {
                                            bestApartmentClass = curApartmentClass;
                                        }
                                    }
                                }
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(country + ";" + bestApartmentClass + ";" + bestCost + ";" + durationRental);
                    order.setConversationId("rent-apartment");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("rent-apartment"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println(country + ";" + bestApartmentClass + ";" + bestCost + ";" + durationRental
                                    + " успешно приобретен у агента "
                                    + reply.getSender().getName());
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Попытка не удалась: запрошенные апартаменты уже сняты с аренды.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Попытка не удалась: " + customerRequests.toString() + " недоступен для аренды.");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }
}