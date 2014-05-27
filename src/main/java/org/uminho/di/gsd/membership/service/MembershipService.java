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

package org.uminho.di.gsd.membership.service;

import org.uminho.di.gsd.membership.service.operations.UpdateOperation;
import org.uminho.di.gsd.membership.service.operations.GetTargetsOperation;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.types.URI;

public class MembershipService extends DefaultService {
	static Logger logger = Logger.getLogger(MembershipService.class);

	private MembershipRepository repository;

	private UpdateOperation updateOp;
	private GetTargetsOperation getTargetsOp;

	public MembershipService()
	{
		super();

		this.setServiceId(new URI(Constants.MembershipServiceName));

		initializeOperations();
	}

	public MembershipRepository getRepository()
	{
		return repository;
	}

	public void setRepository(MembershipRepository repository)
	{
		this.repository = repository;

		getTargetsOp.setRepository(repository);
		updateOp.setRepository(repository);
	}

	private void initializeOperations()
	{
		updateOp = new UpdateOperation();
		this.addOperation(updateOp);

		getTargetsOp = new GetTargetsOperation();
		this.addOperation(getTargetsOp);
	}

}
