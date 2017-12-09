//storage variables
boolean toggle0 = 0;
boolean toggle1 = 0;
boolean toggle2 = 0;

void setup() {

  //set pins as outputs
  pinMode(8, OUTPUT);
  pinMode(9, OUTPUT);

  cli();//stop interrupts

  //set timer0 interrupt at 2kHz
  TCCR0A = 0;// set entire TCCR2A register to 0
  TCCR0B = 0;// same for TCCR2B
  TCNT0  = 0;//initialize counter value to 0
  // set compare match register for 2khz increments
  OCR0A = 125;// = (16*10^6) / (2000*64) - 1 (must be <256)
  // turn on CTC mode
  TCCR0A |= (1 << WGM01);
  // Set CS01 and CS00 bits for 64 prescaler
  TCCR0B |= (1 << CS01) | (1 << CS00);   
  // enable timer compare interrupt
  TIMSK0 |= (1 << OCIE0A);

  //set timer2 interrupt at 8kHz
  TCCR2A = 0;// set entire TCCR2A register to 0
  TCCR2B = 0;// same for TCCR2B
  TCNT2  = 0;//initialize counter value to 0
  // set compare match register for 8khz increments
  OCR2A = 249;// = (16*10^6) / (8000*8) - 1 (must be <256)
  // turn on CTC mode
  TCCR2A |= (1 << WGM21);
  // Set CS21 bit for 8 prescaler
  TCCR2B |= (1 << CS21);
  // enable timer compare interrupt
  TIMSK2 |= (1 << OCIE2A);


  sei();//allow interrupts

}//end setup

ISR(TIMER0_COMPA_vect) { //timer0 interrupt 2kHz toggles pin 8
  //generates pulse wave of frequency 2kHz/2 = 1kHz (takes two cycles for full wave- toggle high then toggle low)
  if (toggle0) {
    digitalWrite(8, HIGH);
    toggle0 = 0;
  }
  else {
    digitalWrite(8, LOW);
    toggle0 = 1;
  }
}

ISR(TIMER2_COMPA_vect) { //timer1 interrupt 8kHz toggles pin 9
  //generates pulse wave of frequency 8kHz/2 = 4kHz (takes two cycles for full wave- toggle high then toggle low)
  if (toggle2) {
    digitalWrite(9, HIGH);
    toggle2 = 0;
  }
  else {
    digitalWrite(9, LOW);
    toggle2 = 1;
  }
}


void loop() {
  //do other things here
}
