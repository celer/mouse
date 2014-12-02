//=========================================================================
//
//  This skeleton was generated by Mouse 1.6.1 at 2014-12-01 21:27:05 GMT
//  from grammar
//    '/home/celer/Project/gomouse/examples/examples2/myGrammar.txt'.
//
//=========================================================================

package main;

import "fmt"
import mouse "../../src/runtime";

type MySemantics struct {
	result float64;  
	mouse.SemanticsBase
}
func NewMySemantics() *MySemantics{
  s:=MySemantics{}
  return &s;
}
//---------------------------------------------------------------------
//  Sum = Number ("+" Number)* !_
//---------------------------------------------------------------------
func (this *MySemantics) sum() {
      n:=this.RHSSize();
			s,ok:=this.RHS(2).Get().(float64);
			if(ok){
				if (!this.RHS(1).IsEmpty()){ s = -s; }
				for i:=4;i<n;i+=2 {
					if (this.RHS(i-1).RuneAt(0)==rune('+')){
						r,ok:=this.RHS(i).Get().(float64);
						if(ok){ s+=r };
					} else {
						r,ok:=this.RHS(i).Get().(float64);
						if(ok){ s-=r };
					}
				}
				this.result=s;
			}
}

//---------------------------------------------------------------------
//  Number = [0-9]+
//---------------------------------------------------------------------
func (this *MySemantics) fraction() {
	var i float64;
	fmt.Sscanf(this.RHSText(0,this.RHSSize()-1),"%f",&i);
	this.LHS().Put(i);
}

//---------------------------------------------------------------------
//  Number = [0-9]+
//---------------------------------------------------------------------
func (this *MySemantics) integer() {
	var i float64;
	fmt.Sscanf(this.LHS().Text(),"%f",&i);
	this.LHS().Put(i);
}

