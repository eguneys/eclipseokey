package main.java;

import com.shephertz.app42.server.idomain.BaseTurnRoomAdaptor;
import com.shephertz.app42.server.idomain.HandlingResult;
import com.shephertz.app42.server.idomain.ITurnBasedRoom;
import com.shephertz.app42.server.idomain.IUser;
import com.shephertz.app42.server.idomain.IZone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RummyRoomExtension3User extends BaseTurnRoomAdaptor {

    private ITurnBasedRoom gameRoom;
    private IZone izone;
    ArrayList<IUser> pausedUserList = new ArrayList<IUser>();
    
    ArrayList<IUser> readyUserList = new ArrayList<IUser>();
    
    // GameData
    private ArrayList<Integer> CARDS_DECK = new ArrayList<Integer>();
    
    private ArrayList<Integer> USER_1_HAND = new ArrayList<Integer>();
    private ArrayList<Integer> USER_2_HAND = new ArrayList<Integer>();
    private ArrayList<Integer> USER_3_HAND = new ArrayList<Integer>();
    private ArrayList<Integer> USER_4_HAND = new ArrayList<Integer>();
    
    
    private ArrayList<Integer> USER_1_DRAW = new ArrayList<Integer>();
    private ArrayList<Integer> USER_2_DRAW = new ArrayList<Integer>();
    private ArrayList<Integer> USER_3_DRAW = new ArrayList<Integer>();
    private ArrayList<Integer> USER_4_DRAW = new ArrayList<Integer>();   
    private Integer GOSTERGE_CARD = -1;
            
    private String user1_name = null;
    private String user2_name = null;
    private String user3_name = null;
    private String user4_name = null;

    public byte MAX_NO_OF_CARDS = 13;
    
    public byte GAME_STATUS;
    
    private Integer GameRestartTime = -1;
    

    
    public RummyRoomExtension3User(IZone izone, ITurnBasedRoom room){
        this.gameRoom = room;
        this.izone = izone;
        GAME_STATUS = CardsConstants.STOPPED;
    }
    
    /*
     * This function is invoked when server receive a move request.
     */
    @Override
    public void handleMoveRequest(IUser sender, String moveData, HandlingResult result){
        try{
        	int moveType = -1;
        	JSONObject data = new JSONObject(moveData);
        	moveType = data.getInt("type");
        	
        	switch (moveType) {
        	case CardsConstants.ME_DRAW_CARD_MIDDLE: case CardsConstants.ME_DRAW_CARD_SIDE: {
        		result.doDefaultTurnLogic = false;
        		result.sendNotification = true;
        		validateAndHandleDraw(sender, moveType, result);
        	} break;
        	case CardsConstants.ME_THROW_CARD: {
        		int card = data.getInt("card");
        		validateAndHandleThrow(sender, card, result);
        	} break;
        	case CardsConstants.ME_FINISHED: {
        		result.doDefaultTurnLogic = false;
        		// TODO
        		JSONArray cards =  data.getJSONArray("cards");
        		int card = data.getInt("card");
        		validateAndHandleFinish(sender, cards, card, result);
        	} break;
        	}
        		// replace card array on server
//        		JSONArray cards = data.getJSONArray("cards");
//        		if(sender.getName().equals(user1_name)){
//        			for(int i=0;i<cards.length();i++){
//        				USER_1_HAND.set(i, cards.getInt(i));
//        			}
//        		}else if(sender.getName().equals(user2_name)){
//        			for(int i=0;i<cards.length();i++){
//        				USER_2_HAND.set(i, cards.getInt(i));
//        			}
//        		}else if(sender.getName().equals(user3_name)){
//        			for(int i=0;i<cards.length();i++){
//        				USER_3_HAND.set(i, cards.getInt(i));
//        			}
//        		}		
        }catch(Exception e){
            e.printStackTrace();
        }
        printAll("handleMoveRequest", true);
    }
    
    
    /*
     * This function is invoked when server receive a chat request.
     */
    @Override
    public void handleChatRequest(IUser sender, String message, HandlingResult result){
        //result.code = CardsConstants.SUBMIT_CARD;
//        try{
//            JSONArray cards = new JSONArray(message);
//            ArrayList cardList = new ArrayList();
//            for(int i=0;i<cards.length();i++){
//                cardList.add(cards.get(i));
//            }
//           boolean status = Utils.checkForWin(cardList);
//            if(status){// for winning condition
//                if(sender.getName().equals(user1_name)){
//                    handleFinishGame(user1_name, cardList);
//                }else if(sender.getName().equals(user2_name)){
//                    handleFinishGame(user2_name, cardList);
//                }else if(sender.getName().equals(user3_name)){
//                    handleFinishGame(user3_name, cardList);
//                }
//            }else{
//                String desc = CardsConstants.SUBMIT_CARD+"#"+"You don't have winning cards";
//                sender.SendChatNotification(CardsConstants.SERVER_NAME, desc, gameRoom);
//            }
//        }catch(JSONException e){
//            e.printStackTrace();
//            result.description = "Error in fetching data";
//        }
    	if (message.equals("AppWarp2Sync")) {
        	ArrayList<Integer> user_hand = this.GetUserHand(sender.getName());
        	
        	JSONObject tobeSent = new JSONObject();
        	
    		try {
    			tobeSent.put("cards", user_hand);
    			
    		}	catch(JSONException e) {
    			e.printStackTrace();
    		}
    		
    		sender.SendChatNotification("AppWarp2", tobeSent.toString(), gameRoom);
    	} else if (message.equals("AppWarp2Sync ready")) {
    		if (!readyUserList.contains(sender)) {
    			readyUserList.add(sender);
    		}
    	}
    }
    
    /*
     * This function is invoked when server leave user request. In case of three user 
     * server continue game with remaining two users and add the cards of third user in total cards.
     */
    @Override
    public void onUserLeavingTurnRoom(IUser user, HandlingResult result){
    	
    	if (gameRoom.getJoinedUsers().size() == 0) {
    		gameRoom.setAdaptor(null);
    		izone.deleteRoom(gameRoom.getId());
    		gameRoom.stopGame(CardsConstants.SERVER_NAME);
    	} else
    	
        if(GAME_STATUS!=CardsConstants.RUNNING){
        	readyUserList.remove(user);
            return;
        }
        else {
        	GAME_STATUS = CardsConstants.PAUSED;
        	gameRoom.stopGame(CardsConstants.SERVER_NAME);
        }
        
//        if(gameRoom.getJoinedUsers().size()==4){// if three users are playing and one of them left room
//            
//        }else if(gameRoom.getJoinedUsers().size()==1){// if two users are playing and one of them left room
//            String leaveingUser = null;
//            if(user.getName().equals(user1_name)){
//                leaveingUser = user1_name;
//            }else if(user.getName().equals(user2_name)){
//                leaveingUser = user2_name;
//            }else if(user.getName().equals(user3_name)){
//                leaveingUser = user3_name;
//            }
//            String message = "You Win! Enemy "+leaveingUser+" left the room";
//            gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, CardsConstants.RESULT_USER_LEFT+"#"+message);
//            gameRoom.setAdaptor(null);
//            izone.deleteRoom(gameRoom.getId());
//            gameRoom.stopGame(CardsConstants.SERVER_NAME);
//        }
    }
    
    public void onUserPaused(IUser user){
        if(gameRoom.getJoinedUsers().contains(user)){
            pausedUserList.add(user);
            GAME_STATUS = CardsConstants.PAUSED;
            gameRoom.stopGame(CardsConstants.SERVER_NAME);
        }
    }
    
    public void onUserResume(IUser user){
        if(pausedUserList.indexOf(user)!=-1){
            pausedUserList.remove(user);
        }
        if(pausedUserList.isEmpty()){
            GAME_STATUS = CardsConstants.RESUMED;
        }
    }
    
    @Override
    public void onTurnExpired(IUser turn, HandlingResult result) {
    	result.sendResponse = false;
    	
    	ArrayList<Integer> user_hand = this.GetUserHand(turn.getName());

    	if (shouldDrawCard(user_hand)) {
    		Integer card = CARDS_DECK.get(CARDS_DECK.size() - 1);
    		this.validateAndHandleDraw(turn, CardsConstants.ME_DRAW_CARD_MIDDLE, result);
    		
    		JSONObject data = new JSONObject();
    		try {
    		data.put("type", CardsConstants.ME_AUTO_DRAW);
    		} catch (JSONException e) {
    		 e.printStackTrace();	
    		}
    		gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, data.toString());

    	}
    	
    	Integer card = user_hand.get(0);
    	this.validateAndHandleThrow(turn, card, result);
    	
    	JSONObject data = new JSONObject();
    	
    	try {
    	    	data.put("type", CardsConstants.ME_AUTO_THROW);
    	    	data.put("card", card);
    	    	data.put("sender", turn.getName());
    			data.put("nextTurn", gameRoom.getNextTurnUser().getName());
    			data.put("currentTurn", gameRoom.getTurnUser().getName());
    	} catch(JSONException e) {
    		e.printStackTrace();
    	}
    	
    	gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, data.toString());
    	
    }
    
    /*
     * This method deal new hand for each user and send
     * chat message having his cards array
     */
    private void dealNewCards(){
    	// Clear Cards
    	
    	CARDS_DECK.clear();
    	USER_1_HAND.clear();
    	USER_2_HAND.clear();
    	USER_3_HAND.clear();
    	USER_4_HAND.clear();
    	USER_1_DRAW.clear();
    	USER_2_DRAW.clear();
    	USER_3_DRAW.clear();
    	USER_4_DRAW.clear();
    	
    	
    	
    	
    	
        // Build Cards
    	for (int k=0; k<2; k++) {
    		for(int i=1;i<=CardsConstants.MAX_CARD;i++){
    			CARDS_DECK.add(i);
    		}	
    	}
    	GOSTERGE_CARD = CARDS_DECK.remove(0);
    	
		CARDS_DECK.add(CardsConstants.MAX_CARD + 1);  // Fake 1
		CARDS_DECK.add(CardsConstants.MAX_CARD + 1);  // Fake 2
		
    	// Deal cards
    	Collections.shuffle(CARDS_DECK);
        for(int i=0;i<MAX_NO_OF_CARDS;i++){
            USER_1_HAND.add(CARDS_DECK.remove(0));
            USER_2_HAND.add(CARDS_DECK.remove(0));
            USER_3_HAND.add(CARDS_DECK.remove(0));
            USER_4_HAND.add(CARDS_DECK.remove(0));
        }
        
        
        // TODO add one more card to turn player
        
        List<IUser>list = gameRoom.getSubscribedUsers();
        if(list.size()==4){
        	
            IUser iuser1 = list.get(0);
            IUser iuser2 = list.get(1);
            IUser iuser3 = list.get(2);
            IUser iuser4 = list.get(3);
            
            user1_name = iuser1.getName();
            user2_name = iuser2.getName();
            user3_name = iuser3.getName();
            user4_name = iuser4.getName();
            try{
                JSONObject dataUser1 = new JSONObject();
                dataUser1.put(iuser1.getName(), USER_1_HAND);
                JSONObject dataUser2 = new JSONObject();
                dataUser2.put(iuser2.getName(), USER_2_HAND);
                JSONObject dataUser3 = new JSONObject();
                dataUser3.put(iuser3.getName(), USER_3_HAND);
                JSONObject dataUser4 = new JSONObject();
                dataUser4.put(iuser4.getName(), USER_4_HAND);
                
                dataUser1.put("type", CardsConstants.PLAYER_HAND);
                dataUser2.put("type", CardsConstants.PLAYER_HAND);
                dataUser3.put("type", CardsConstants.PLAYER_HAND);
                dataUser4.put("type", CardsConstants.PLAYER_HAND);

                
                iuser1.SendChatNotification(CardsConstants.SERVER_NAME, dataUser1.toString(), gameRoom);
                iuser2.SendChatNotification(CardsConstants.SERVER_NAME, dataUser2.toString(), gameRoom);
                iuser3.SendChatNotification(CardsConstants.SERVER_NAME, dataUser3.toString(), gameRoom);
                iuser4.SendChatNotification(CardsConstants.SERVER_NAME, dataUser4.toString(), gameRoom);
                
                ArrayList<String> userTurns = new ArrayList<String>();
                userTurns.add(user1_name);
                userTurns.add(user2_name);
                userTurns.add(user3_name);
                userTurns.add(user4_name);
                JSONObject dataUsers = new JSONObject();
                dataUsers.put("type", CardsConstants.GAME_STARTINFO);
                dataUsers.put("turns", userTurns);
                dataUsers.put("gosterge", GOSTERGE_CARD);
                
                gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, "" + dataUsers);
                
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        printAll("dealNewCards", true);
    }
    
    private void redealCards() {
        List<IUser>list = gameRoom.getSubscribedUsers();
        if(list.size()==4){
        	
            IUser iuser1 = list.get(0);
            IUser iuser2 = list.get(1);
            IUser iuser3 = list.get(2);
            IUser iuser4 = list.get(3);
            
            user1_name = iuser1.getName();
            user2_name = iuser2.getName();
            user3_name = iuser3.getName();
            user4_name = iuser4.getName();
            try{
                JSONObject dataUser1 = new JSONObject();
                dataUser1.put(iuser1.getName(), USER_1_HAND);
                JSONObject dataUser2 = new JSONObject();
                dataUser2.put(iuser2.getName(), USER_2_HAND);
                JSONObject dataUser3 = new JSONObject();
                dataUser3.put(iuser3.getName(), USER_3_HAND);
                JSONObject dataUser4 = new JSONObject();
                dataUser4.put(iuser4.getName(), USER_4_HAND);
                
                dataUser1.put("type", CardsConstants.PLAYER_HAND);
                dataUser2.put("type", CardsConstants.PLAYER_HAND);
                dataUser3.put("type", CardsConstants.PLAYER_HAND);
                dataUser4.put("type", CardsConstants.PLAYER_HAND);

                
                iuser1.SendChatNotification(CardsConstants.SERVER_NAME, dataUser1.toString(), gameRoom);
                iuser2.SendChatNotification(CardsConstants.SERVER_NAME, dataUser2.toString(), gameRoom);
                iuser3.SendChatNotification(CardsConstants.SERVER_NAME, dataUser3.toString(), gameRoom);
                iuser4.SendChatNotification(CardsConstants.SERVER_NAME, dataUser4.toString(), gameRoom);
                
                ArrayList<String> userTurns = new ArrayList<String>();
                userTurns.add(user1_name);
                userTurns.add(user2_name);
                userTurns.add(user3_name);
                userTurns.add(user4_name);
                JSONObject dataUsers = new JSONObject();
                dataUsers.put("type", CardsConstants.GAME_STARTINFO);
                dataUsers.put("turns", userTurns);
                dataUsers.put("gosterge", GOSTERGE_CARD);
                                
                gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, "" + dataUsers);
                
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void handleTimerTick(long time){
        /*
         * A game when room full
         * or we can say max users are equals to joined users
         */
        
        if(GAME_STATUS==CardsConstants.STOPPED && gameRoom.getJoinedUsers().size()==gameRoom.getMaxUsers() && readyUserList.size() == gameRoom.getMaxUsers()){
            GAME_STATUS=CardsConstants.RUNNING;
            dealNewCards();
            gameRoom.startGame(CardsConstants.SERVER_NAME);
        }else if(GAME_STATUS==CardsConstants.RESUMED){
            GAME_STATUS=CardsConstants.RUNNING;
            gameRoom.startGame(CardsConstants.SERVER_NAME);
        } else if (GAME_STATUS == CardsConstants.PAUSED && gameRoom.getJoinedUsers().size() == gameRoom.getMaxUsers() && readyUserList.size() == gameRoom.getMaxUsers()) {
        	GAME_STATUS = CardsConstants.RUNNING;
        	redealCards();
        	gameRoom.startGame(CardsConstants.SERVER_NAME);
        } else if (GAME_STATUS == CardsConstants.FINISHED) {
        	if (GameRestartTime < 0) {
        		GameRestartTime = 10;
        	} else if (GameRestartTime > 0) {
        		--GameRestartTime;
        	} else {
        		GameRestartTime = -1;
        		GAME_STATUS= CardsConstants.STOPPED;
        	}
        }
    }
    
    private ArrayList<Integer> GetUserHand(String name) {
    	ArrayList<Integer> user_hand = null;
    	if(name.equals(user1_name)) {
    		user_hand = USER_1_HAND;
    	} else if(name.equals(user2_name)) {
    		user_hand = USER_2_HAND;
        } else if(name.equals(user3_name)) {
        	user_hand = USER_3_HAND;
        } else if(name.equals(user4_name)) {
        	user_hand = USER_4_HAND;
        }
		return user_hand;
    }
    
    
    
    /*
     * This method return last element of TOTAL_CARDS
     * In case of empty list again shuffle cards
     */
    private Integer getNewCard(){
        if(CARDS_DECK.isEmpty()){
        	// finish game
        	System.out.println("End Game");
        	return -1;
        } else
        	return CARDS_DECK.remove(CARDS_DECK.size()-1);
     }
    
    private boolean shouldDrawCard(ArrayList<Integer> hand) {
    	return (hand.size() < MAX_NO_OF_CARDS + 1);
    }
            
    private void validateAndHandleDraw(IUser sender, int drawType, HandlingResult result) {
    	ArrayList<Integer> USER_HAND = null, DRAW_HAND = null;
    	if (sender.getName().equals(user1_name)) {
    	    			USER_HAND = USER_1_HAND;
    	    			DRAW_HAND = USER_1_DRAW;
    	} else if (sender.getName().equals(user2_name)) {
    					USER_HAND = USER_2_HAND;
    					DRAW_HAND = USER_2_DRAW;
    	} else if (sender.getName().equals(user3_name)) {
    					USER_HAND = USER_3_HAND;
    					DRAW_HAND = USER_3_DRAW;
    	} else if (sender.getName().equals(user4_name)) {
    					USER_HAND = USER_4_HAND;
    					DRAW_HAND = USER_4_DRAW;
    	} 
    	

    	{	
    		if (!shouldDrawCard(USER_HAND)) {
    			result.code = CardsConstants.INVALID_MOVE;
    			result.description = "Too many cards";
    		} else {
    			if (drawType == CardsConstants.ME_DRAW_CARD_SIDE) {
    				if (DRAW_HAND.size() == 0) {
    					result.code = CardsConstants.INVALID_MOVE;
    					result.description = "Invalid Move";
    				} else {
    					USER_HAND.add(DRAW_HAND.remove(DRAW_HAND.size() - 1));

    				}
    			} else {
    				
    			    int newCard = getNewCard();
    				// TODO END GAME
    			    if (newCard == -1) {
    			    	
    			    	JSONObject tobeSent = new JSONObject();
    			    	
    			    	
    			    	
    			    	gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, tobeSent.toString());
    			    	dealNewCards();
    			    	
    			    	return;
    			    }
    			    
    				USER_HAND.add(newCard);
    				
    				try {
    					JSONObject tobeSent = new JSONObject();
    					
    					tobeSent.put("type", CardsConstants.ME_DRAW_CARD_MIDDLE_INFO);
    					tobeSent.put("card", newCard);
    				    tobeSent.put("pad", 101010);
    					sender.SendChatNotification(CardsConstants.SERVER_NAME, tobeSent.toString(), gameRoom);
    				} catch(JSONException e) {
    					e.printStackTrace();
    				}
    			}
    		}
    		
    		if (result.code != CardsConstants.INVALID_MOVE) {
    			JSONObject tobeSent = new JSONObject();    			
    			
    			try {
    				
    				tobeSent.put("type", drawType);
    				tobeSent.put("side", sender.getName());
    				
    				gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, tobeSent.toString());
    			}	 catch(JSONException e) {
    				e.printStackTrace();
    			}
    		}
    	}
    }
    
    private void validateAndHandleThrow(IUser sender, int card, HandlingResult result) {
    	ArrayList<Integer> USER_HAND = null, THROW_HAND = null;
    	if (sender.getName().equals(user1_name)) {
    	    			USER_HAND = USER_1_HAND;
    	    			THROW_HAND = USER_2_DRAW;
    	} else if (sender.getName().equals(user2_name)) {
    					USER_HAND = USER_2_HAND;
    					THROW_HAND = USER_3_DRAW;
    	} else if (sender.getName().equals(user3_name)) {
    					USER_HAND = USER_3_HAND;
    					THROW_HAND = USER_4_DRAW;
    	} else if (sender.getName().equals(user4_name)) {
    					USER_HAND = USER_4_HAND;
    					THROW_HAND = USER_1_DRAW;
    	} 

    	
    	{    		
    		if (shouldDrawCard(USER_HAND)) {
    			result.code = CardsConstants.INVALID_MOVE;
    			result.description = "Draw a card first";
    		} else {
    			if (!USER_HAND.contains(card)) {
    				result.code = CardsConstants.INVALID_MOVE;
    				result.description = "No such card";
    			} else {
    				THROW_HAND.add(USER_HAND.remove(USER_HAND.indexOf(card)));
    			}
    		}
    	}
    }
    
    
    private void validateAndHandleFinish(IUser sender, JSONArray cards, int card, HandlingResult result) {
    	ArrayList<Integer> USER_HAND = null;
    	if (sender.getName().equals(user1_name)) {
    	    			USER_HAND = USER_1_HAND;
    	} else if (sender.getName().equals(user2_name)) {
    					USER_HAND = USER_2_HAND;
    	} else if (sender.getName().equals(user3_name)) {
    					USER_HAND = USER_3_HAND;
    	} else if (sender.getName().equals(user4_name)) {
    					USER_HAND = USER_4_HAND;
    	} 
    	
    	
    	ArrayList<Integer> hand = new ArrayList<Integer>(USER_HAND);

    	if (Utils.checkFinish(hand, cards, card, Utils.cardPlusOne(GOSTERGE_CARD))) {
    		GAME_STATUS = CardsConstants.FINISHED;
    		gameRoom.stopGame(CardsConstants.SERVER_NAME);
    		
    		JSONObject finishData = new JSONObject();
    		
    		try {
    			finishData.put("type", CardsConstants.ME_FINISHED);
    			finishData.put("data", cards);
    		} catch(JSONException e) {
    			e.printStackTrace();
    		}
    		
    		gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, finishData.toString());

    	}	
    }
    
    /*
     * This function stop the game and notify the room players about winning user and his cards.
     */
    private void handleFinishGame(String winningUser, ArrayList<Integer> cards){
        try{
            JSONObject object = new JSONObject();
            object.put("win", winningUser);
            object.put("cards", cards);
            GAME_STATUS = CardsConstants.FINISHED;
            gameRoom.BroadcastChat(CardsConstants.SERVER_NAME, CardsConstants.RESULT_GAME_OVER+"#"+object);
            gameRoom.setAdaptor(null);
            izone.deleteRoom(gameRoom.getId());
            gameRoom.stopGame(CardsConstants.SERVER_NAME);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // for debugging 
    
    private void printAll(String TAG, boolean status){
        if(status){
            System.out.println("==================="+TAG+"======================");
            System.out.println("USER_1:   "+USER_1_HAND);
            System.out.println("USER_2:   "+USER_2_HAND);
            System.out.println("USER_3:   "+USER_3_HAND);
            System.out.println("USER_4:   "+USER_4_HAND);
            System.out.println("TOTAL_CA: "+CARDS_DECK);
        }
    }
}
