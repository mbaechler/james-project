package org.apache.james.webadmin.routes;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.Service;

public class MailQueueRoutes implements Routes {

    @VisibleForTesting  static final String BASE_URL = "/mailQueues";
    private final MailQueueFactory mailQueueFactory;
    private final JsonTransformer jsonTransformer;

    @Inject
    @VisibleForTesting MailQueueRoutes(MailQueueFactory mailQueueFactory, JsonTransformer jsonTransformer) {
        this.mailQueueFactory = mailQueueFactory;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define(Service service) {
        defineListQueues(service);
    }

    @GET
    @ApiOperation(
        value = "Listing existing MailQueues"
    )
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.OK_200, message = "OK", response = List.class),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineListQueues(Service service) {
        service.get(BASE_URL,
            (request, response) ->
                mailQueueFactory
                    .getUsedMailQueues()
                    .stream()
                    .map(MailQueue::getMailQueueName)
                    .collect(Guavate.toImmutableList()),
            jsonTransformer);
    }
}
