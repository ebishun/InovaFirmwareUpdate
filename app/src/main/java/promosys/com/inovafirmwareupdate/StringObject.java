package promosys.com.inovafirmwareupdate;

public class StringObject {

  public int index;
  public String sendStr;

  public StringObject(){}

  public StringObject(int index,String sendStr){
    this.index = index;
    this.sendStr = sendStr;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public String getSendStr() {
    return sendStr;
  }

  public void setSendStr(String sendStr) {
    this.sendStr = sendStr;
  }
}