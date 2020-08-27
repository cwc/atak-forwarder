package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufModel;

public class ModelProtobufConverter {
    private static final String KEY_MODEL = "model";

    private static final String KEY_NAME = "name";

    public ProtobufModel.MinimalModel toModel(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufModel.MinimalModel.Builder builder =  ProtobufModel.MinimalModel.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NAME:
                    builder.setName(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: model." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddModel(CotDetail cotDetail, ProtobufModel.MinimalModel model) {
        if (model != null && model != ProtobufModel.MinimalModel.getDefaultInstance()) {
            CotDetail modelDetail = new CotDetail(KEY_MODEL);

            if (!StringUtils.isNullOrEmpty(model.getName())) {
                modelDetail.setAttribute(KEY_NAME, model.getName());
            }

            cotDetail.addChild(modelDetail);
        }
    }

}