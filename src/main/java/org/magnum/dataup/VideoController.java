/*
 * 
 * Copyright 2014 Jules White
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
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class VideoController{
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	private static AtomicLong currentId = new AtomicLong(1L);
    private VideoFileManager videoDataMgr;
    
    
    public VideoController(){
    	
    	System.out.println("start conrtoller");
    	/*try {
			videoDataMgr = VideoFileManager.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/	
    }
    
    
	
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideoMetaData(@RequestBody Video video) {
		
		//if (video.getId() == 0){
			long id = genId(); 
			String url = getDataUrl(id);
			
			video.setId(id);
			video.setDataUrl(url);
			videos.put(id, video);
		//}
		
	    return video; 
	}

	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile data,
			HttpServletResponse response) throws IOException{
	
		    Video video; 
		    VideoStatus status = new VideoStatus(VideoState.PROCESSING);
		    
		    if (videos.containsKey(id)){
		       video = videos.get(id);
		       videoDataMgr = VideoFileManager.get();
			   videoDataMgr.saveVideoData(video,  data.getInputStream());
			   response.setStatus(200);
			   status.setState(VideoState.READY);
		    }
		    else{
		        response.sendError(404, "id is not found");
		    }
		    
		return status;
	}

    @RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
	public void getData(
			@PathVariable("id") long id,
			HttpServletResponse response) throws IOException{
    	
    	if (videos.containsKey(id)){
    		Video video = videos.get(id);
    		OutputStream outputStream = response.getOutputStream();
    		videoDataMgr.copyVideoData(video,  outputStream);
    		response.setStatus(200);
    	}
    	else{
    		response.sendError(404);
    	}
	}
	
	//************************************************
	
	private long genId(){
		return currentId.incrementAndGet();
	}

	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }
	
	private String getUrlBaseForLocalServer() {
        HttpServletRequest request = 
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base = 
           "http://"+request.getServerName() 
           + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
     }
	
	
}
