///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.mapstruct:mapstruct:1.5.3.Final
//DEPS org.mapstruct:mapstruct-processor:1.5.3.Final
//JAVAC_OPTIONS -Amapstruct.verbose=true -verbose -Xlint

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

public class mapstruct {

    public static enum CarType {
        SEDAN, CAMPER, X4, TRUCK;
    }

    static public class Car {

        public Car(String string, int i, mapstruct.CarType sedan) {
            this.make = string;
            this.numberOfSeats = i;
            this.type = sedan;
        }

        public String make;
        public int numberOfSeats;
        public CarType type;
    }

    static public class CarDto {
        public String make;
        public int seatCount;
        public String type;
    }

    @Mapper
    static public interface CarMapper {

        CarMapper INSTANCE = Mappers.getMapper(CarMapper.class);

        @Mapping(source = "numberOfSeats", target = "seatCount")
        CarDto carToCarDto(Car car);
    }

    public static void main(String... args) {
        // given
        Car car = new Car("Morris", 5, CarType.SEDAN);

        // when
        CarDto carDto = CarMapper.INSTANCE.carToCarDto(car);

        System.out.println(carDto.make);
        System.out.println(carDto.seatCount);
        System.out.println(carDto.type);
    }
}
