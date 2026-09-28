package com.lld;

import com.lld.Problem.*;
import java.util.*;

public class Catalog {
    private final Map<String, Problem> problems = new LinkedHashMap<>();

    public Catalog(Collection<Problem> ps) { ps.forEach(p -> problems.put(p.id(), p)); }

    public Problem get(String id) {
        Problem p = problems.get(id);
        if (p == null) throw new Errors.NotFound("problem '" + id + "' not found");
        return p;
    }
    public List<Problem> list() { return List.copyOf(problems.values()); }

    public static Catalog defaultCatalog() {
        Problem parking = new Problem("parking-lot", "Parking Lot", "A multi-floor parking lot that issues tickets and charges on exit.", "easy",
            List.of("Vehicles of different types (bike, car, truck) park in spots of matching size.",
                    "The lot has multiple floors; each floor has many spots.",
                    "Entering vehicles get a ticket; leaving vehicles pay a fee based on duration.",
                    "Pricing rules must be changeable (hourly today, flat/weekend rates tomorrow).",
                    "The rule for choosing which spot to give a vehicle must be swappable."),
            List.of(Concept.of("Parking lot", "an entry point that owns floors and coordinates parking", "parkinglot", "garage", "parkinggarage"),
                    Concept.of("Parking spot", "spots have a size/type and an occupied state", "parkingspot", "spot", "slot", "space"),
                    Concept.of("Vehicle", "vehicle types drive spot compatibility", "vehicle", "car", "bike", "truck"),
                    Concept.of("Ticket", "links a vehicle, a spot and an entry time", "ticket", "receipt", "parkingticket")),
            List.of(VariationPoint.of("pricing", "Fee calculation", "Put fee rules behind an interface (Strategy) so new rate plans need no edits to the lot.",
                        "pricing", "fee", "rate", "tariff", "charge", "cost"),
                    VariationPoint.of("allocation", "Spot allocation", "Choosing a spot (nearest, cheapest, lowest floor) is a policy; keep it separate from the lot.",
                        "allocation", "allocator", "assign", "spotfinder", "selection")),
            List.of(Behaviour.of("Park a vehicle", "park", "enter", "issue", "checkin"),
                    Behaviour.of("Unpark / exit", "unpark", "exit", "leave", "checkout", "release", "remove"),
                    Behaviour.of("Calculate fee", "fee", "price", "cost", "calculate", "charge", "amount")));

        Problem elevator = new Problem("elevator", "Elevator System", "A building with several elevators serving floor and in-car requests.", "hard",
            List.of("A building has N floors and M elevators.",
                    "Passengers call an elevator from a floor (up/down) and pick a destination inside it.",
                    "A controller decides which elevator serves which request.",
                    "Elevators move, stop, open/close doors and can be idle, moving up or moving down.",
                    "The dispatch algorithm must be replaceable (nearest car, least loaded, zoning)."),
            List.of(Concept.of("Elevator", "holds current floor, direction and pending stops", "elevator", "cabin", "liftcar", "lift"),
                    Concept.of("Controller / dispatcher", "central coordination of requests across elevators", "controller", "dispatcher", "scheduler", "elevatorsystem", "manager"),
                    Concept.of("Request", "a request has a floor and direction; modelling it makes queues and dispatch clean", "request", "call", "hallcall", "stoprequest")),
            List.of(VariationPoint.of("dispatch", "Dispatch algorithm", "Model dispatch as a Strategy so 'nearest car' can be swapped for other policies.",
                        "dispatch", "scheduling", "scheduler", "algorithm", "assign", "selection", "selector"),
                    VariationPoint.of("state", "Elevator state", "Idle/moving/door-open behave differently: a State pattern avoids nested ifs.",
                        "state", "idle", "moving", "direction")),
            List.of(Behaviour.of("Request an elevator", "request", "call", "press", "summon"),
                    Behaviour.of("Move / step", "move", "step", "tick", "advance", "run"),
                    Behaviour.of("Open / close door", "door", "open", "close")));

        Problem vending = new Problem("vending-machine", "Vending Machine", "A machine that sells products for coins/notes and can refund.", "easy",
            List.of("The machine stocks products in slots with quantity and price.",
                    "A user inserts money, selects a product, and receives the product and change.",
                    "The user can cancel and get a refund before dispensing.",
                    "Behaviour depends on machine state (idle, has money, dispensing, out of service).",
                    "Payment methods (cash now, card later) must be extendable."),
            List.of(Concept.of("Vending machine", "the facade the user interacts with", "vendingmachine", "machine"),
                    Concept.of("Product", "name, price, identity", "product", "item", "snack"),
                    Concept.of("Inventory", "tracks quantity per product", "inventory", "stock", "slot", "shelf")),
            List.of(VariationPoint.of("state", "Machine state", "Which actions are valid depends on state; State pattern keeps 'insert' and 'select' free of if-chains.",
                        "state", "idle", "hasmoney", "dispensing"),
                    VariationPoint.of("payment", "Payment method", "Hide how money is collected behind an interface so cards/wallets can be added.",
                        "payment", "paymentmethod", "cash", "card", "payer")),
            List.of(Behaviour.of("Insert money", "insert", "pay", "addmoney", "deposit"),
                    Behaviour.of("Select product", "select", "choose", "pick"),
                    Behaviour.of("Dispense", "dispense", "deliver", "release"),
                    Behaviour.of("Refund / cancel", "refund", "cancel", "returnmoney")));
        return new Catalog(List.of(parking, elevator, vending));
    }
}
