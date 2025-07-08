package com.ob.api.dx.model.response;

import com.ob.api.dx.model.data.Position;
import lombok.Data;

import java.util.List;

@Data
public class PositionListResponse {
    List<Position> positions;
}
