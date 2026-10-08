package com.openrsc.server.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private final String websocketUri;

	public HttpRequestHandler(String wsUri) {
		websocketUri = wsUri;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (this. websocketUri.equalsIgnoreCase(request.uri())) {                                                                                                                      
			ctx.fireChannelRead(request.retain());                                                                                          
		} else {
			                                                                       
			HttpResponse response = new DefaultHttpResponse(
				request.protocolVersion(), HttpResponseStatus.OK);
			ctx.write(response);
			ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
		throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
