package travelAgency;

public class Record {
    public String country;
    public String apartmentClass;
    public Integer cost;
    public Integer durationRental;

    public Record(String arg, String arg1, Integer arg2, Integer arg3) {
        this.country = arg;
        this.apartmentClass = arg1;
        this.cost = arg2;
        this.durationRental = arg3;
    }

    public String String() {
        return "страна: " + this.country + "; класс апартаментов: " + this.apartmentClass + "; стоимость: " +
                this.cost + "; длительность съема: " + this.durationRental + ";";
    }

    public String toString() {
        return this.country + ";" + this.apartmentClass + ";" + this.cost + ";" + this.durationRental;
    }
}
