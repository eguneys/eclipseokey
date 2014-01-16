package main.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    /*
     * This function accept three card (id) and check if they are of same type
     */
    public static boolean checkForSameType(List<Integer> list){
        Collections.sort(list); 
        int c1 = list.get(0);
        int c2 = list.get(1);
        int c3 = list.get(2);
        if((c2==(c1+13) || c2==(c1+26) || c2==(c1+39)) && (c3==(c1+13) || c3==(c1+26) || c3==(c1+39))){
            return true;
        }
        return false;
    }
    
    /*
     * This function accept three card (id) and check if they are in sequence
     */
    public static boolean checkForSuquence(List<Integer> list){
        int c1 = list.get(0);
        int c2 = list.get(1);
        int c3 = list.get(2);
        if((c1!=12 || c1!=25 || c1!=38 || c1!=51) && c1%13!=0){
            if(c2==(c1+1) && c3==(c1+2)){
                return true;
            }
        }else if(c1==12 || c1==25 || c1==38 || c1==51){
            int r = 0;
            if(c1>12){
                r = c1/12;
                r--;
            }
            if(c2==(c1+1) && c3==1+(r*13)){
                return true;
            }
            
        }
        else if(c1%13==0){// correct
            int r = c1/13;
            r--;
            if(c2==1+(r*13) && c3==2+(r*13)){
                return true;
            }
        }
        return false;
    }
    
    public static boolean checkForWin(ArrayList<Integer> list){
        int winCounter = 0;
        int startIndex = 0;
        for(int i=0;i<3;i++){
            List<Integer> subList = list.subList(startIndex, startIndex+3);
            if((checkForSameType(subList))||(checkForSuquence(subList))){
                winCounter++;
            }
            startIndex +=3;
        }
        if(list.size()==6 && winCounter==2){// three user case
            return true;
        }
        if(list.size()==9 && winCounter==3){// two user case
            return true;
        }
        return false;
    }

	public static boolean checkFinish(ArrayList<Integer> hand,
			JSONArray cards, int card, int fake) {
		
		try {
		
			int count = cards.length();
			
			while (--count >= 0) {
				JSONArray group = cards.getJSONArray(count);
				
				if (group.length() == 1) {
					if (group.getInt(0) != card) return false;
					card = -1;
					continue;
				}
				JSONArray okeyedgroup = okeyify(group, fake);
				if (!isValidGroup(okeyedgroup, fake)) {
					return false;
				}
				
				// does hand contain group elements
				for (int i = 0; i< group.length(); i++) {
					if (!hand.contains(group.getInt(i))) {
						return false;
					}
					hand.remove(hand.indexOf(group.getInt(i)));
				}
			}
			
		} catch(JSONException e) {
			return false;
		}
		
		return true;
		
		
	}

	private static JSONArray okeyify(JSONArray group, int fake) throws JSONException {
		JSONArray result = new JSONArray();
		
		for (int i = group.length() - 1; i>=0; --i) {
			int item = group.getInt(i);
			if (item == fake) {
				result.put(54);
			} else {
				result.put(item);
			}
		}
		
		return result;
		
	}

	public static boolean isValidGroup(JSONArray group, int fake) throws JSONException {
		if (isValidSameNumber(group, fake) || isValidSerial(group, fake))
			return true;
		return false;
	}
	
	public static boolean isValidSameNumber(JSONArray group, int fake) throws JSONException {
		if (group.length() < 3) return false;
		
		int a = group.getInt(0);
		int b = group.getInt(1);
		int c = group.getInt(2);
		
		if (isSameNumber(a, b, fake) && isSameNumber(b, c, fake)) {
			if (group.length() == 4) {
				int d = group.getInt(3);
				
				if (isSameNumber(c, d, fake)) {
					return a != b && a!=c && a!=d && b!=c && c!=d;
				} else return false;
			} else 
				return a != b && b != c && a != c;
			
		} else {
			return false;
		}
		
	}
	
	public static boolean isValidSerial(JSONArray group, int fake) throws JSONException {
		if (group.length()<3) return false;
		
		boolean isIncreasing = false;
		
		if (isValidIncreasing(group.getInt(0), group.getInt(1), fake) && isValidIncreasing(group.getInt(1), group.getInt(2), fake)) isIncreasing = true;
		else if (!isValidDecreasing(group.getInt(0), group.getInt(1), fake)) return false;
		
		int len = group.length();
		
		while (--len >= 1) {
			if (isIncreasing) {
				if (!isValidIncreasing(group.getInt(len - 1), group.getInt(len), fake)) return false;
			} else {
				if (!isValidDecreasing(group.getInt(len - 1), group.getInt(len), fake)) return false;
			}	
		}
		return true;
	}
	
	
	
	
	public static boolean isSameColor(int card1, int card2, int fake) {
		if (card1 == 53) return isSameColor(fake, card2, fake);
		else if (card2 == 53) return isSameColor(card1, fake, fake);
		else if (card1 == 54 || card2 == 54) return true;
		else return ((card1 <= 13 && card2 <= 13) || (card1 <= 13 * 2 && card2 <= 13 * 2) || (card1 <= 13 * 3 && card2 <= 13*3) || (card1 <= 13 * 4 && card2 <= 13*4));
	}
	
	public static boolean isSameNumber(int card1, int card2, int fake) {
		if (card1 == 53) return isSameNumber(fake, card2, fake);
		if (card2 == 53) return isSameNumber(card1, fake, fake);
		if (card1 == 54 || card2 == 54) return true;
		
		int big, small; 
		
		if (card1 < card2) { big = card2; small = card1; }
		else if (card2 < card1) { big = card1; small = card2; }
		else return true;
		
		if (small + 13 == big || small + 13 * 2 == big || small + 13 * 3 == big) return true;
		else return false;
	}
	
	public static boolean isValidIncreasing(int card1, int card2, int fake) {
		if (card1 == 54 || card2 == 54) return true;
		if (card1 == 53) return isValidIncreasing(fake, card2, fake);
		if (card2 == 53) return isValidIncreasing(card1, fake, fake);
		
		if (isSameColor(card1+1, card2, fake) && isSameNumber(card1+1, card2, fake)) return true;
		return false;
	}
	

	public static boolean isValidDecreasing(int card1, int card2, int fake) {
		if (card1 == 54 || card2 == 54) return true;
		if (card1 == 53) return isValidDecreasing(fake, card2, fake);
		if (card2 == 53) return isValidDecreasing(card1, fake, fake);
		
		if (isSameColor(card1-1, card2, fake) && isSameNumber(card1-1, card2, fake)) return true;
		return false;
	}
	
	public static int cardPlusOne(int card) {
		if (card == 13 || card == 26 || card == 39 || card == 52) return card - 12;
		return card + 1;
	}
		
}