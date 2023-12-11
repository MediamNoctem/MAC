package warehouseSupply;

public class Record {
    public String productName;
    public String category;
    public int count;

    public Record(String arg, String arg1, Integer arg2) {
        this.productName = arg;
        this.category = arg1;
        this.count = arg2;
    }

    public String String() {
        return "название товара: " + this.productName + "; категория товара: " + this.category + "; количество: " +
                this.count + ";";
    }
}
