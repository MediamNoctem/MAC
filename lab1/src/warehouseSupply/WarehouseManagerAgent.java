package warehouseSupply;

import com.sun.jdi.Value;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

// Агент-менеджер склада
public class WarehouseManagerAgent extends Agent {
    public String[] category;
    // {"Хлебобулочные изделия", "Бакалея", "Крупы", "Мясо", "Молочные изделия"};
    public ArrayList<Record> productDatabase;
    ArrayList<String> listOfNecessaryProducts;
    private AID[] supplyAgents;

    protected void setup() {
        System.out.println("Агент-менеджер склада " + getAID().getName() + " готов.\n");

        Record record;
        HashSet<String> categoryHashSet = new HashSet<>();
        Object[] args = getArguments();

        if (args != null && args.length > 0) {
            System.out.println("-----------------База товаров менеджера------------------");
            for (int i = 0; i < args.length / 3; i++) {
                record = new Record((String) args[3 * i], (String) args[3 * i + 1],
                        Integer.parseInt((String) args[3 * i + 2]));
                productDatabase.add(record);
                categoryHashSet.add((String) args[3 * i + 1]);
                System.out.println("Запись " + i + ":\n" + record.String());
            }
            System.out.println("---------------------------------------------------------\n");

            category = categoryHashSet.toArray(new String[0]);

            addBehaviour(new TickerBehaviour(this, 20000) {
                @Override
                protected void onTick() {
                    System.out.println("Осуществляется случайный расход товара.");
                    randomProductConsumption(productDatabase, category);

                    System.out.println("Проверяем, нужно ли пополнить товарные позиции.");
                    listOfNecessaryProducts = makeListOfNecessaryProductsLessN(productDatabase,
                            category, 10);

                    if (!listOfNecessaryProducts.isEmpty()) {
                        System.out.println("Необходимо пополнить следующие товарные позиции:");
                        for (String product : listOfNecessaryProducts) {
                            System.out.println("    * " + product);
                        }

                        System.out.println("Поиск агентов снабжения.");
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("supply-of-products");
                        template.addServices(sd);
                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            System.out.println("Найдены следующие агенты снабжения:");
                            supplyAgents = new AID[result.length];
                            for (int i = 0; i < result.length; ++i) {
                                supplyAgents[i] = result[i].getName();
                                System.out.println("    * " + supplyAgents[i].getName());
                            }
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }

                        myAgent.addBehaviour(new RequestPerformer());
                    } else {
                        System.out.println("На данный момент товарные позиции пополнить не нужно.");
                    }
                }
            });
        }
        else {
            System.out.println("Не указаны товарные позиции.");
            doDelete();
        }
    }

    protected void takeDown() {
        System.out.println("Агент-менеджер " + getAID().getName() + " завершил свою работу.");
    }

    // Случайный расход товара
    public void randomProductConsumption(ArrayList<Record> db, String[] category) {
        Random random = new Random();
        int randomCategory = random.nextInt(category.length);
        int randomProduct = random.nextInt(numberProductsInCategory(db, category[randomCategory]));
        int numberProductInDB = findNumberProductByNumberInCategory(db, category[randomCategory], randomProduct);
        int randomProductConsumption = random.nextInt(numberProductInDB);
        db.get(numberProductInDB).count -= randomProductConsumption;
    }

    // Поиск количества товарной позиции по номеру в категории
    public int findNumberProductByNumberInCategory (ArrayList<Record> db, String category, int numberInCategory) {
        int counter = 0;
        for (Record record : db) {
            if (record.category.compareTo(category) == 0) {
                counter++;
                if (counter == numberInCategory) {
                    return record.count;
                }
            }
        }
        throw new RuntimeException("Ошибка поиска количества товарной позиции по номеру в категории!");
    }

    // Посчитать количество товарных позиций в категории
    public int numberProductsInCategory(ArrayList<Record> db, String category) {
        int number = 0;
        for (Record record : db) {
            if (record.category.compareTo(category) == 0) {
                number += 1;
            }
        }
        return number;
    }

    // Составляем список товарных позиций по категориям, количество которых меньше n, если таких позиций не менее трех
    // в категории
    public ArrayList<String> makeListOfNecessaryProductsLessN (ArrayList<Record> db, String[] category, int n) {
        ArrayList<ArrayList<String>> listOfNecessaryProductsByCategory = makeListOfNecessaryProducts(db, category, n);
        ArrayList<String> listOfNecessaryProductsLessN = new ArrayList<>();
        for (ArrayList<String> products : listOfNecessaryProductsByCategory) {
            if (products.size() >= 3) {
                listOfNecessaryProductsLessN.addAll(products);
            }
        }
        return listOfNecessaryProductsLessN;
    }

    // Составление общего списка необходимых товаров
    public ArrayList<ArrayList<String>> makeListOfNecessaryProducts (ArrayList<Record> db, String[] category, int n) {
        ArrayList<ArrayList<String>> listOfNecessaryProductsByCategory = new ArrayList<>();
        for (String c : category) {
            ArrayList<String> listOfNecessaryProducts = findProductsInCategoryLessN(db, c, n);
            listOfNecessaryProductsByCategory.add(listOfNecessaryProducts);
        }
        return listOfNecessaryProductsByCategory;
    }

    // Поиск необходимых товаров в одной категории, количество которых меньше n
    public ArrayList<String> findProductsInCategoryLessN(ArrayList<Record> db, String category, int n) {
        Record record;
        ArrayList<String> listOfNecessaryProductsInCategory = new ArrayList<>();
        for (Record value : db) {
            record = value;
            if (record.category.compareTo(category) == 0) {
                if (record.count <= n) {
                    listOfNecessaryProductsInCategory.add(record.productName);
                }
            }
        }
        return listOfNecessaryProductsInCategory;
    }

    public String arrayListToString (ArrayList<String> list) {
        String string = "";
        for (String s : list) {
            string += s + ";";
        }
        return string;
    }

    private class RequestPerformer extends Behaviour {
        private AID supplier;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    Random random = new Random();
                    int randomNumberSupplyAgent = random.nextInt(supplyAgents.length);

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(supplyAgents[randomNumberSupplyAgent]);
                    cfp.setContent(arrayListToString(listOfNecessaryProducts));
                    cfp.setConversationId("supply-of-products");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());

                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("supply-of-products"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // String[] listOfFoundProducts = reply.getContent().split(";");
                            supplier = reply.getSender();
                        }
                        step = 2;
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(supplier);
                    order.setContent(arrayListToString(listOfNecessaryProducts));
                    order.setConversationId("supply-of-products");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("supply-of-products"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("Товары успешно приобретены через агента снабжения " + reply.getSender().getName());
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Попытка не удалась: запрошенных товаров нет.");
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
            if (step == 2 && supplier == null) {
                System.out.println("Ошибка: запрошенных товаров нет.");
            }
            return ((step == 2 && supplier == null) || step == 4);
        }
    }
}
