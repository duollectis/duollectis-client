package org.duollectis.client.api.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cancellable {

	private boolean canceled;

	public void cancel() {
		canceled = true;
	}
}
