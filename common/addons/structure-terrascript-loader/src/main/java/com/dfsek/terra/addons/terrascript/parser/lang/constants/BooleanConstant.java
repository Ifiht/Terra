package com.dfsek.terra.addons.terrascript.parser.lang.constants;

import com.dfsek.terra.addons.terrascript.api.Position;
import com.dfsek.terra.addons.terrascript.api.lang.ConstantExpression;

public class BooleanConstant extends ConstantExpression<Boolean> {
    public BooleanConstant(Boolean constant, Position position) {
        super(constant, position);
    }

    @Override
    public ReturnType returnType() {
        return ReturnType.BOOLEAN;
    }
}
