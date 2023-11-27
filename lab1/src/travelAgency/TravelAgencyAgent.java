package travelAgency;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;


public class TravelAgencyAgent extends Agent {
    private static final long serialVersionUID = 1L;

    private ArrayList<Record> catalogue;
    Record record;

    protected void setup() {
        System.out.println("Привет! Турагенство " + getAID().getName() + " готово.");

        catalogue = new ArrayList<>();
        Object[] args = getArguments();

        for (int i = 0; i < args.length / 4; i++) {
            if (args.length > 0) {
                record = new Record((String) args[4 * i], (String) args[4 * i + 1], Integer.parseInt((String) args[4 * i + 2]),
                        Integer.parseInt((String) args[4 * i + 3]));
                catalogue.add(record);
                System.out.println("Запись " + i + ":\n" + record.String());
            }
        }

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

        addBehaviour(new TravelAgencyAgent.OfferRequestsServer());
        addBehaviour(new TravelAgencyAgent.PurchaseOrdersServer());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("TravelAgency-agent "+getAID().getName()+" завершил свою работу.");
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String[] customerRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();

                ArrayList<Record> response = new ArrayList<>();
                Record cur;

                for (int i = 0; i < catalogue.size(); i++) {
                    cur = catalogue.get(i);
//                    System.out.println(cur.country.compareTo(customerRequests[0]) == 0);
//                    System.out.println(cur.apartmentClass.compareTo(customerRequests[1]) >= 0);
//                    System.out.println(cur.cost <= Integer.parseInt(customerRequests[2]));
//                    System.out.println(cur.durationRental >= Integer.parseInt(customerRequests[3]));
                    if (cur.country.compareTo(customerRequests[0]) == 0){
                        if (cur.apartmentClass.compareTo(customerRequests[1]) >= 0) {
                            if (cur.cost <= Integer.parseInt(customerRequests[2])) {
                                if (cur.durationRental >= Integer.parseInt(customerRequests[3])) {
                                    response.add(cur);
                                }
                            }
                        }
                    }
                }

                if (!response.isEmpty()) {
                    String answers = "";
                    for (int i = 0; i < response.size(); i++) {
                        answers += response.get(i).toString() + ";";
                    }
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(answers);
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String[] customerRequests = msg.getContent().split(";");
                ACLMessage reply = msg.createReply();
                Record cur = null;
                Boolean flag = false;
                int tmp;

                for (int i = 0; i < catalogue.size(); i++) {
                    cur = catalogue.get(i);
                    if (cur.country.compareTo(customerRequests[0]) == 0){
                        if (cur.apartmentClass.compareTo(customerRequests[1]) == 0) {
                            if (cur.cost == Integer.parseInt(customerRequests[2])) {
                                if (cur.durationRental >= Integer.parseInt(customerRequests[3])) {
                                    flag = true;
                                    tmp = cur.durationRental - Integer.parseInt(customerRequests[3]);
                                    cur.durationRental = Integer.parseInt(customerRequests[3]);
                                    catalogue.remove(i);
                                    if (tmp != 0) {
                                        catalogue.add(new Record(cur.country, cur.apartmentClass, cur.cost, tmp));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                if (flag) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(cur.toString() + " был продан агенту " + msg.getSender().getName());
                }
                else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
}
