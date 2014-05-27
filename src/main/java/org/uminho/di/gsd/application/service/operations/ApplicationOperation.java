/*******************************************************************************
 * Copyright (c) 2014 Filipe Campos.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.uminho.di.gsd.application.service.operations;

import org.uminho.di.gsd.application.service.ApplicationService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.types.QName;

public abstract class ApplicationOperation extends Operation {

    public ApplicationService service;

    public ApplicationOperation(String opName, QName qname, ApplicationService svc)
    {
        super(opName, qname);

        service = svc;
    }
}
