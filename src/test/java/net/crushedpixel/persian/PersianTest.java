package net.crushedpixel.persian;

import net.crushedpixel.persian.annotations.Access;
import net.crushedpixel.persian.annotations.Model;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static net.crushedpixel.persian.annotations.Access.AccessType.METHOD;

public class PersianTest {

    @Model
    public static class TestModel {
        List<Person> people = new ArrayList<>();
    }

    @Model
    public static class Person {
        String name;
        int age;
        boolean gender;

        List<Hobby> hobbies = new ArrayList<>();

        // tests circular references
        List<Person> friends = new ArrayList<>();

        public Person() {
        }
    }

    @Model
    public static class Hobby {
        String name;
        Price price;

        public Hobby() {
        }

        public Hobby(String name, Price price) {
            this.name = name;
            this.price = price;
        }

        @Access(METHOD)
        String getName() {
            return "Hobby " + this.name;
        }

        void setName(String name) {
            this.name = name.substring("Hobby ".length());
        }
    }

    public static class Price {
        String unit;
        float amount;

        public Price() {
        }

        public Price(String unit, float amount) {
            this.unit = unit;
            this.amount = amount;
        }
    }

    @Test
    public void testSerialization() throws Exception {
        var model = new TestModel();

        var swimmingHobby = new Hobby("Swimming", new Price("€", 3.5f));
        var programmingHobby = new Hobby("Programming", new Price("$", 0));
        var climbingHobby = new Hobby("Climbing", new Price("€", 18));

        var person1 = new Person();
        model.people.add(person1);

        {
            person1.name = "Marius";
            person1.age = 20;
            person1.gender = true;
            person1.hobbies.add(programmingHobby);
            person1.hobbies.add(climbingHobby);
        }

        var person2 = new Person();
        model.people.add(person2);

        {
            person2.name = "Günther";
            person2.age = 43;
            person2.gender = true;
            person2.hobbies.add(swimmingHobby);
            person2.hobbies.add(climbingHobby);
        }

        // test circular references
        person2.friends.add(person1);
        person1.friends.add(person2);

        String json = Persian.serialize(model);
        System.out.println(json);
    }

    @Test
    public void testDeserialization() throws Exception {
        var model = Persian.deserialize("{\"root\":{\"id\":0},\"models\":[{\"type\":\"net.crushedpixel.persian.PersianTest$TestModel\",\"value\":{\"people\":[{\"id\":1},{\"id\":2}]}},{\"type\":\"net.crushedpixel.persian.PersianTest$Person\",\"value\":{\"name\":\"Marius\",\"gender\":true,\"hobbies\":[{\"id\":3},{\"id\":4}],\"age\":20,\"friends\":[{\"id\":2}]}},{\"type\":\"net.crushedpixel.persian.PersianTest$Person\",\"value\":{\"name\":\"Günther\",\"gender\":true,\"hobbies\":[{\"id\":5},{\"id\":4}],\"age\":43,\"friends\":[{\"id\":1}]}},{\"type\":\"net.crushedpixel.persian.PersianTest$Hobby\",\"value\":{\"name\":\"Hobby Programming\",\"price\":{\"unit\":\"$\",\"amount\":0.0}}},{\"type\":\"net.crushedpixel.persian.PersianTest$Hobby\",\"value\":{\"name\":\"Hobby Climbing\",\"price\":{\"unit\":\"€\",\"amount\":18.0}}},{\"type\":\"net.crushedpixel.persian.PersianTest$Hobby\",\"value\":{\"name\":\"Hobby Swimming\",\"price\":{\"unit\":\"€\",\"amount\":3.5}}}]}",
                TestModel.class);

        System.out.println(model);
    }

}
