package prgrms.neoike.service.dto.drawdto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public record ServiceItemDto(
        @NotNull
        @PositiveOrZero
        int size,

        @NotNull
        @PositiveOrZero
        int quantity
){
}
