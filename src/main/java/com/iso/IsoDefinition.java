package com.iso;

import lombok.Getter;
import lombok.Setter;

/**
 * IsoDefinition class represents the definition of an ISO data element.
 * It includes the type of data element, its size, length definition, and name.
 */
@Getter
@Setter
public class IsoDefinition {

    private IsoTypeDataElementEnum typeDataElement;
    private int typeElementSize;
    private int lengthDefinition;
    private String name;

    public IsoDefinition(IsoTypeDataElementEnum typeDataElement, int lengthDefinition,  String name) {
        this.typeDataElement = typeDataElement;
        this.lengthDefinition = lengthDefinition;
        this.name = name;

        if (typeDataElement.equals(IsoTypeDataElementEnum.LLVAR)){
            this.typeElementSize = 2;
        }else if (typeDataElement.equals(IsoTypeDataElementEnum.LLLVAR)){
            this.typeElementSize = 3;
        }else if (typeDataElement.equals(IsoTypeDataElementEnum.FIXED)){
            this.typeElementSize = 0;
        }
    }
}
