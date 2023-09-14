package B;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class BClass extends Agent {
    protected void setup() {
        System.out.println("Привет! Агент " + getAID().getLocalName() + " готов.");
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println(" – " + myAgent.getLocalName()
                            + " получил сообщение: \"" + msg.getContent()
                            + "\" от " + msg.getSender().getLocalName());
                    //Вывод на экран локального имени агента и полученного сообщения
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Pong"); //Содержимое сообщения
                    send(reply); //отправляем сообщения
                }
                block();
            }
        });
    }
}
