package com.ob.broker.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkSession {
    private DayOfWeek day;
    private long start;
    private long end;

    @Getter
    public enum DayOfWeek {
        SUNDAY(0),
        MONDAY(1),
        TUESDAY(2),
        WEDNESDAY(3),
        THURSDAY(4),
        FRIDAY(5),
        SATURDAY(6);
        private final int value;

        DayOfWeek(int value) {
            this.value = value;
        }

        public static DayOfWeek fromValue(int value) {
            return switch (value) {
                case 0 -> SUNDAY;
                case 1 -> MONDAY;
                case 2 -> TUESDAY;
                case 3 -> WEDNESDAY;
                case 4 -> THURSDAY;
                case 5 -> FRIDAY;
                case 6 -> SATURDAY;
                default -> SUNDAY;
            };
        }
    }
}
