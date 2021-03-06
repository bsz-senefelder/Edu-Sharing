package org.edu_sharing.restservices.clientutils.v1;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.clientutils.v1.model.WebsiteInformation;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.clientutils.ClientUtilsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/clientUtils/v1")
@Api(tags = {"CLIENTUTILS v1"})
@ApiService(value="CLIENTUTILS", major=1, minor=0)
public class ClientUtilsApi  {

	
	private static Logger logger = Logger.getLogger(ClientUtilsApi.class);
	
    @GET
    @Path("/getWebsiteInformation")        
    @ApiOperation(
    	value = "Read generic information about a webpage")
    
    @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = WebsiteInformation.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getWebsiteInformation(
    	
    	@ApiParam(value = "full url with http or https" ) @QueryParam("url") String url,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	WebsiteInformation info=new WebsiteInformation(ClientUtilsService.getWebsiteInformation(url));
	    	return Response.status(Response.Status.OK).entity(info).build();
	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@OPTIONS    
    @Path("/getWebsiteInformation")
    @ApiOperation(hidden = true, value = "")

	public Response options01() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
}

