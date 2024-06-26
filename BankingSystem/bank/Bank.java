package BankingSystem.bank;

import BankingSystem.account.Account;
import BankingSystem.account.EAccount;
import BankingSystem.account.SaveAccount;
import BankingSystem.utility.Dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class Bank {
    private CentralBank centralBank;

    public void associate(CentralBank centralBank) {
        this.centralBank = centralBank;
    }

    enum EState{
        CONNECTED,
        DISCONNECTED
    }

    enum EMenuTable{
        RETURN_PREV("이전으로"),
        CREATE_ACCOUNT("계좌생성"),
        WITHDRAW("출금"),
        DEPOSIT("입금"),
        REMIT("송금"),
        GET_ACCOUNT_INFO("계좌정보 확인");

        public String getMenuName() {
            return menuName;
        }

        private final String menuName;



        EMenuTable(String menuName) {
            this.menuName = menuName;
        }

        public static String[] nameValues(){
            int nOfValues = values().length;
            String[] returnValue = new String[nOfValues];
            for(int i = 0; i < nOfValues; i++){
                returnValue[i] = values()[i].getMenuName();
            }
            return returnValue;
        }
    }

//    private String[] menuTable = { "이전으로", "계좌생성", "출금", "입금", "송금", "계좌정보 확인" };
    private static int nOfBanks = 0;

    private int bankNum;

    private EState eState;
    private int nOfAccounts;


    public int getBankNum() {
        return bankNum;
    }

    public Bank(){
        nOfAccounts = 0;
        bankNum = nOfBanks++;
        eState = EState.CONNECTED;
        System.out.println(bankNum);
    }

    public void createAccount(){

        //계좌종류 선택
        Dialog.systemMsg("계좌 종류를 선택하세요");
//        Dialog.showMenus(EAccount.values());
//        EAccount eAccount = EAccount.values()[Dialog.selectInRange(EAccount.values().length)];
        EAccount eAccount = EAccount.values()[Dialog.selectMenu(EAccount.nameValues())];



        Account newAccount = eAccount.getAccount().newAccount();
        String name = Dialog.inputAsString("예금주를 입력하세요");
        newAccount.setAccountHolder(name);


        if (eAccount == EAccount.S){
            //이 부분을 좀더 개선할수 있을듯? 상위클래스에서 additionalSettings() 하고 msg셋과 리턴밸류 타입을 저장해두면 일반화가능할듯
            //additionalSettings(String[] args, EType[] argTypes)로 주면 될듯
            BigDecimal targetAmount = Dialog.inputAsBigDecimal("목표금액을 설정해주세요");
            ((SaveAccount) newAccount).setTargetAmount(targetAmount);
        }

        String accountNum = createAccountNum(eAccount);
        newAccount.setAccountNum(accountNum);
        //TODO: 2024-03-30   boolean 으로 등록완료 판별과정+@ 구현필요
        centralBank.registerAccount(newAccount);
        Dialog.systemMsg("계좌생성이 완료되었습니다. ");
        System.out.println(newAccount.getAccountInfo());

    }


    public void selectMenu(){
        EMenuTable selectedMenu = EMenuTable.values()[Dialog.selectMenu(EMenuTable.nameValues())];
        switch (selectedMenu){
            case RETURN_PREV -> returnPrev();
            case CREATE_ACCOUNT -> createAccount();
            case WITHDRAW -> withdraw();
            case DEPOSIT -> deposit();
            case REMIT -> remit();
            case GET_ACCOUNT_INFO -> getAccountInfo();
        }
    }

    private void returnPrev() {
        eState = EState.DISCONNECTED;
    }

    private void getAccountInfo() {
        Account account = getAccount();
        System.out.println(account.getAccountInfo());
    }

    private Account getAccount(String msg) {
        Account account = centralBank.getAccount(Dialog.inputAsAccountNum(msg + "계좌번호를 입력하세요"));
        if (account == null){
            Dialog.systemMsg("유요하지 않은 계좌입니다. 다시 입력하세요");
            account = centralBank.getAccount(Dialog.inputAsAccountNum(msg + "계좌번호를 입력하세요"));
        }
        return account;
    }
    private Account getAccount(){
        return getAccount("");
    }

    private void remit() {
        //출금이랑 입금 메서드를 일반화 시키면 그 두개를 활용해서 간단히 할수 있을듯.
        Account sendAccount = getAccount("보내시는 분의 ");
        BigDecimal amount = Dialog.inputAsBigDecimal("송금할 금액을 입력해주세요");
        if(sendAccount.withdraw(amount)){
            Account receiveAccount = getAccount("받으시는 분의 ");
            if (receiveAccount.deposit(amount)){
                Dialog.systemMsg("송금이 완료되었습니다.");
                Dialog.systemMsg("현재 잔액: " + sendAccount.getBalance());
            }else {
                Dialog.systemMsg("송금 실패");
                // 보내는사람계좌에서는 출금된 상태에서 송금실패시 다시 입금해야함.
                sendAccount.deposit(amount);
            }
        }else {
            Dialog.systemMsg("송금 실패");
        }

    }

    private void deposit() {
        Account account = getAccount();
        BigDecimal amount = Dialog.inputAsBigDecimal("입금할 금액을 입력해주세요");
        if (account.deposit(amount)){
            Dialog.systemMsg("입금이 완료되었습니다.");
            Dialog.systemMsg("현재 잔액: " + account.getBalance());
        }else {
            Dialog.systemMsg("입금 실패");
        }
    }

    private void withdraw() {
        Account account = getAccount();
        BigDecimal amount = Dialog.inputAsBigDecimal("출금할 금액을 입력해주세요");
        if(account.withdraw(amount)){
            Dialog.systemMsg("출금이 완료되었습니다.");
            Dialog.systemMsg("현재 잔액: " + account.getBalance());
        }else {
            Dialog.systemMsg("출금 실패");
        }

    }

    private String createAccountNum(EAccount eAccount){
        String accountNum = "";
        accountNum += String.format("%03d", bankNum) +
                String.format("%02d", eAccount.getAccountTypeCode()) +
                String.format("%05d", nOfAccounts++);
        accountNum += createVerifyNum(accountNum);
        return accountNum;
    }

    private String createVerifyNum(String accountNum) {
        // generate by gpt
        int[] weights = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4}; // 각 숫자에 곱해질 가중치

        int sum = 0;
        for (int i = 0; i < accountNum.length(); i++) {
            char digit = accountNum.charAt(i);
            if (Character.isDigit(digit)) {
                int num = Character.getNumericValue(digit);
                sum += num * weights[i];
            } else {
                // 숫자가 아닌 경우 처리
                // 여기서는 예외처리 없이 그냥 패스하도록 함
            }
        }

        int remainder = sum % 11;
        int verifyNum;
        if (remainder == 0)
            verifyNum = 0;
        else if (remainder == 10)
            verifyNum = 'X';
        else
            verifyNum = 11 - remainder;

        return String.valueOf(verifyNum);
    }
    public void payInterest(){
        ArrayList<Account> accountList = centralBank.getAllAccounts(bankNum);
        InterestCalculator interestCalculator;
        BigDecimal sum = new BigDecimal(0);
        for(Account account : accountList){
            interestCalculator = centralBank.interestCalculatorTable.get(account.getEAccount());
            BigDecimal interest = interestCalculator.getInterest(account.getBalance()).setScale(0, RoundingMode.UP); //소수점은 올림
            sum = sum.add(interest);
            account.setBalance(account.getBalance().add(interest));
        }
        Dialog.systemMsg(accountList.size() + "개의 계좌에 이자 지급을 완료했습니다.");
        Dialog.systemMsg("총 " + sum + "원을 지급했습니다.");
    }

    public void run() {
        eState = EState.CONNECTED;
        while (eState == EState.CONNECTED){
            selectMenu();
        }
    }
}
