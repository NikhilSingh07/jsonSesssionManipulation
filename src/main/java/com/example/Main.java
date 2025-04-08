package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args)  throws Exception{
        
        String jsonResponse = getJson();
        ObjectMapper mapper = new ObjectMapper();
        EventsWrapper eventsWrapper = mapper.readValue(jsonResponse, EventsWrapper.class);
        List<Event> events = eventsWrapper.events;

        // 1. Group sessions based on the visited id
        Map<String, List<Event>> groupedOnVisitorId = new HashMap<>();
        for(Event e: events) {
            String visitorId = e.visitorId;
            if(!groupedOnVisitorId.containsKey(e.visitorId)) {
                groupedOnVisitorId.put(e.visitorId, new ArrayList<>());
            }
            groupedOnVisitorId.get(e.visitorId).add(e);
        }


        Map<String, List<Session>> visitorSessionsMap = new HashMap<>();
        // 2. Events should be sorted based on timestamp.
        for( Map.Entry<String, List<Event>> mapEntry :groupedOnVisitorId.entrySet()) {

            String visitorID = mapEntry.getKey();
            List<Event> sortedEvents= mapEntry.getValue();

            Collections.sort(sortedEvents, new Comparator<Event>() {
                @Override
                public int compare(Event e1, Event e2) {
                    return Long.compare(e1.timestamp, e2.timestamp);
                }
                
            });

            
            // 3. List of sessions where each session is a group of events with no more than 10 mins b/w 2 events.

            Long  timeDuraiton = 0L;
            Long sessionStartTime = -1L;
            List<String> listPages = new ArrayList<>();
            List<Session> visiotorSessions = new ArrayList<>();

            for(int i=0; i<sortedEvents.size(); i++) {

                Long currentEventTime = sortedEvents.get(i).timestamp;

                // if it is the first event of the session.
                if(sessionStartTime == -1) {
                    sessionStartTime = currentEventTime;
                }

                // add the event to the session.
                listPages.add(sortedEvents.get(i).url);

                // if there's a next event, check the time difference.
                if(i+1 <  sortedEvents.size()) {
                    Long nextEventTime = sortedEvents.get(i+1).timestamp;

                    // if the time diff is within 10 min, continue the session.
                    if(nextEventTime - currentEventTime <= 10*60*1000) {
                        timeDuraiton = nextEventTime - sessionStartTime;
  
                    } else {
                    
                    // end the session.    
                        Session session = new Session(timeDuraiton, new ArrayList<>(listPages), sessionStartTime);
                        visiotorSessions.add(session);

                        // Reset for the new session.
                        listPages.clear();
                        timeDuraiton = 0L;
                        sessionStartTime = -1L;
    
                    }

                } else { // handle the last event

                    timeDuraiton = currentEventTime - sessionStartTime;
                    Session session = new Session(timeDuraiton, new ArrayList<>(listPages), sessionStartTime);
                    visiotorSessions.add(session);

                }
            }

            visitorSessionsMap.put(visitorID, visiotorSessions);
            // 10 30 25 40 (10, 20,)
    
        }

        SessionByUsers sessionByUsers = new SessionByUsers(visitorSessionsMap);

        System.out.println(sessionByUsers);



    }

    private static record SessionByUsers(Map<String, List<Session>> sessionUsers) { }
    private static record Session(Long duration, List<String> pages, Long startTime) {}

    private static record EventsWrapper(List<Event> events) {}
    private static record Event(String url, String visitorId, Long timestamp) {}

    private static String getJson(){
        // allows to create multiline string w/o adding \n
        String json = """
            {
                "events": [
                    {
                        "url": "/pages/a-big-river",
                        "visitorId": "d1177368-2310-11e8-9e2a-9b860a0d9039",
                        "timestamp": 1512754583000
                    },
                    {
                        "url": "/pages/a-small-dog",
                        "visitorId": "d1177368-2310-11e8-9e2a-9b860a0d9039",
                        "timestamp": 1512754631000
                    },
                    {
                        "url": "/pages/a-big-talk",
                        "visitorId": "f877b96c-9969-4abc-bbe2-54b17d030f8b",
                        "timestamp": 1512709065294
                    },
                    {
                        "url": "/pages/a-sad-story",
                        "visitorId": "f877b96c-9969-4abc-bbe2-54b17d030f8b",
                        "timestamp": 1512711000000
                    },
                    {
                        "url": "/pages/a-big-river",
                        "visitorId": "d1177368-2310-11e8-9e2a-9b860a0d9039",
                        "timestamp": 1512754436000
                    },
                    {
                        "url": "/pages/a-sad-story",
                        "visitorId": "f877b96c-9969-4abc-bbe2-54b17d030f8b",
                        "timestamp": 1512709024000
                    }
                ]
            }
                """;
        
        return json;        
    }
}