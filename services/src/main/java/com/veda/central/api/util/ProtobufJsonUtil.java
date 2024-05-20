/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.veda.central.api.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.veda.central.api.exception.ProtobufConversionException;

public class ProtobufJsonUtil {

    /**
     * Converts a JSON string to a Protobuf message builder.
     *
     * @param json    the JSON string
     * @param builder the Protobuf message builder
     * @param <T>     the type of the Protobuf message
     * @return the Protobuf message builder
     * @throws ProtobufConversionException if there is an error during parsing
     */
    public static <T extends Message.Builder> T jsonToProtobuf(String json, T builder) {
        try {
            JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
            return builder;

        } catch (InvalidProtocolBufferException e) {
            throw new ProtobufConversionException("Failed to convert JSON to Protobuf", e);
        }
    }

    /**
     * Converts a Protobuf message to a JSON string.
     *
     * @param message the Protobuf message
     * @param <T>     the type of the Protobuf message
     * @return the JSON string
     * @throws ProtobufConversionException if there is an error during conversion
     */
    public static <T extends Message> String protobufToJson(T message) {
        try {
            return JsonFormat.printer().print(message);

        } catch (InvalidProtocolBufferException e) {
            throw new ProtobufConversionException("Failed to convert Protobuf to JSON", e);
        }
    }
}
