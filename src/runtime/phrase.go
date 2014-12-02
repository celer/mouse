package mouse;

import "fmt"

type Phrase struct {
	base *ParserBase;
	name string;
	diag string;
	start int;
	end int;
	success bool;
	rhs []*Phrase;
	value interface{}
	parent *Phrase;
	errPos int
	errTxt []string;
}

func NewPhrase(base *ParserBase, name string, diag string, start int) *Phrase {
	p:=Phrase{};
	p.base=base;
	p.name=name;
	p.diag=diag;
	p.start=start;
	p.end=start;

	p.errPos=-1;
	p.errTxt=make([]string,0,10);
	p.rhs=make([]*Phrase,0,10);

	return &p;
}

func (p *Phrase) Put(value interface {}) { p.value=value };
func (p *Phrase) Get() interface{} { return p.value };
func (p *Phrase) Text() string { 
	return p.base.source.At(p.start,p.end); 
};
func (p *Phrase) RuneAt(i int) rune { return p.base.source.RuneAt(p.start+i); };
func (p *Phrase) Rule() string { return p.name };
func (p *Phrase) IsA(ruleName string) bool { return p.name==ruleName; }
func (p *Phrase) IsTerminal() bool { return len(p.name)==0; }
func (p *Phrase) IsEmpty() bool { return p.start==p.end; }
func (p *Phrase) ErrMsg() string {
	if (p.errPos<0){  return "" }
	return p.base.source.Where(p.errPos)+":"+p.listErr();
}
func (p *Phrase) ErrClear() {
	p.errTxt=make([]string,0,10);
	p.errPos=-1;
}
func (p *Phrase) Where(i int) string { return p.base.source.Where(i); }
func (p *Phrase) ErrSet(who string, where int){
	p.errTxt=make([]string,0,10);
	p.errTxt=append(p.errTxt,who);
	p.errPos=where;
}
func (p *Phrase) ErrAdd(who string){
	if p.errPos>p.base.pos { return; }
	if p.errPos<p.base.pos {
		p.errTxt=make([]string,0,10);
		p.errPos=p.base.pos;
		p.errTxt=append(p.errTxt,who);
		return;
	}
	p.errTxt=append(p.errTxt,who)
}

//-----------------------------------------------------------------
//  Merge error info with with that from Phrase 'p'.
//-----------------------------------------------------------------
func (this *Phrase) errMerge(p *Phrase) {
	if (p.errPos<this.base.pos && this.errPos<this.base.pos){ // If we passed all error points
		this.ErrClear();
		return;
	}

	if (p.errPos<0){ return; }        // If no error in p: forget
	if (this.errPos>p.errPos){ return; }   // If error in p older: forget
	if (this.errPos<p.errPos){            // If error in p newer: replace all info
		this.errTxt=make([]string,0);
		this.errPos = p.errPos;
		for _,e:=range p.errTxt {
			this.errTxt=append(this.errTxt,e);
		}
		return;
	}
	for _,e:=range p.errTxt {
		this.errTxt=append(this.errTxt,e);
	}
}

func (p *Phrase) listErr() string {
	one:="";
	two:="";
	done:=make([]string,0);
	for _,s:=range p.errTxt {
		stop:=false;
		for _,d:=range done {
			if (d==s){
				stop=true;
			}
		}
		if(stop) {
			continue;
		}
		done=append(done,s);
		if (len(s)>3 && s[0:4]=="not "){
			two=two+fmt.Sprintf(" or %s",s);
		} else {
			one=one+fmt.Sprintf(" or %s",s);
		}
	}

	if (len(one)>0) {
		if (len(two)==0){
			return " expected " + one[4:];
		} else {
			return " expected " + one[4:] + "; not expected " + two[4:];
		}
	} else {
		return " not expected " + two[4:];
	}
}

