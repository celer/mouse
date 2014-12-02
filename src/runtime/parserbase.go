package mouse;

import "fmt"
import "strings"
import "unicode/utf8"
import "errors"

type ParserBase struct {
	source Source;
	endpos int;
	pos int;
	trace string;
	current *Phrase;
}

func (p *ParserBase) Init(source Source){
	p.source=source;
	p.pos=0;
	p.endpos = source.End();
	p.current = NewPhrase(p,"","",0);
}

func (p ParserBase) LHS() *Phrase {
	return p.current;
}

func (p ParserBase) RHS(i int) *Phrase {
	return p.current.rhs[i];
}
func (p ParserBase) RHSSize() int{
	return len(p.current.rhs);
}

func (p ParserBase) RHSText(i int, j int) string {
	if j<=i { return "" }
	return p.source.At(p.RHS(i).start,p.RHS(j-1).end);
}

func (p *ParserBase) setTrace(trace string){
	p.trace=trace;
	//FIXME
	//p.sem.trace=trace;
}

func (p *ParserBase) Failure() (bool,error) {
	if (p.current.errPos>0){
		//FIXME should log so that it can be shown, etc
		return false,errors.New(p.current.ErrMsg());
	}
	return false,errors.New("Parse error");
}

func (p *ParserBase) Begin(name string){
	pr:=NewPhrase(p,name,name,p.pos);
	pr.parent=p.current;
	p.current = pr;
}

func (p *ParserBase) BeginWithDiag(name string,diag string){
	pr:=NewPhrase(p,name,name,p.pos);
	pr.parent=p.current;
	p.current = pr;
}

func (p *ParserBase) Accept() bool {
	pr:=p.pop();
	pr.rhs = nil;
	if (pr.errPos==pr.start){
		pr.ErrSet(pr.diag,pr.start);
	}
	pr.success=true;
	p.current.end = p.pos;
	p.current.rhs=append(p.current.rhs,pr);
	p.current.errMerge(pr);
	return true;
}

func (p *ParserBase) AcceptInner() bool {
	phrase:=p.pop();
	phrase.success=true;
	p.current.end=p.pos;
	for _,r:=range phrase.rhs {
		p.current.rhs=append(p.current.rhs,r);
	}
	p.current.errMerge(phrase);
	return true;
}

func (p *ParserBase) AcceptNot() bool {
	phrase:=p.pop();
	phrase.rhs=nil;
	phrase.ErrClear();
	phrase.success=true;
	return true;
}

func (p *ParserBase) AcceptAnd() bool {
	phrase:=p.pop();
	phrase.end=phrase.start;
	phrase.rhs=nil;
	phrase.ErrClear();
	phrase.success=true;
	p.pos=phrase.start;
	return true;
}

func (p *ParserBase) Reject() bool {
	phrase:=p.pop();
	phrase.end=phrase.start;
	phrase.rhs=nil;
	if phrase.errPos==phrase.start {
		phrase.ErrSet(phrase.diag,phrase.start);
	}
	phrase.success=false;
	p.current.errMerge(phrase);
	return false;
}

func (p *ParserBase) BoolReject() bool{
	p.pos=p.current.start;
	p.current.end = p.pos;
	p.current.rhs=make([]*Phrase,0,10);
	p.current.ErrSet(p.current.diag,p.pos);
	return false;
}

//-------------------------------------------------------------------
//  Reject Inner
//-------------------------------------------------------------------
func (p *ParserBase) RejectInner() bool{
	phrase:=p.pop();
	phrase.end=phrase.start;
	phrase.rhs=nil;
	phrase.success=false;
	//FIXME
	p.current.errMerge(phrase);
	p.pos=phrase.start;
	return false;
}


//-------------------------------------------------------------------
//  Reject And-predicate (argument was rejected)
//  Note: we ignore all failures encountered in processing the argument,
//  and register failure at the point of call of the predicate.
//-------------------------------------------------------------------
func (p *ParserBase) RejectAnd() bool {
		phrase:=p.pop();                // Pop p from compile stack
		phrase.rhs = nil ;                    // Remove right-hand side of p
		phrase.ErrSet(phrase.diag,p.pos);            // Register 'xxx expected'
		phrase.success = false;               // Indicate p failed
		p.current.errMerge(phrase);             // Merge error info with parent
		return false;
	}


//-------------------------------------------------------------------
//  Reject Not-predicate (argument was accepted)
//  Note: we ignore all failures encountered in processing the argument,
//  and register failure at the point of call of the predicate.
//-------------------------------------------------------------------
func (p *ParserBase) RejectNot() bool {
		phrase:=p.pop();                // Pop p from compile stack
		phrase.end = phrase.start;                 // Reset end of p
		phrase.rhs = nil;                    // Remove right-hand side of p
		p.pos = phrase.start;                   // Backtrack to start of p
		phrase.ErrSet(phrase.diag,p.pos);            // Register 'xxx not expected'
		phrase.success = false;               // Indicate p failed
		p.current.errMerge(phrase);             // Merge error info with parent
		return false;
	}


//-------------------------------------------------------------------
//  Execute expression 'c'
//-------------------------------------------------------------------
func (p *ParserBase) NextRune(ch rune) bool {
	if (p.pos<p.endpos && p.source.RuneAt(p.pos)==ch){
		return p.consume(1);
	} else {
		return p.fail(fmt.Sprintf("'%c'",ch));
	}
}

//-------------------------------------------------------------------
//  Execute expression ^'c'
//-------------------------------------------------------------------
func (p *ParserBase) NextNotRune(ch rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)!=ch){
			return p.consume(1);
		} else {
			return p.fail(fmt.Sprintf("not '%c'",ch));
		}
	}

//-------------------------------------------------------------------
//  Execute expression &'c', !^'c'
//-------------------------------------------------------------------
func (p *ParserBase) AheadRune(ch rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)==ch){
			return true;
		} else {
			return p.fail(fmt.Sprintf("'%c'",ch));
		}
	}

func (p *ParserBase) AheadNotNot(ch rune) bool { // temporary
	return p.AheadRune(ch);
}

//-------------------------------------------------------------------
//  Execute expression !'c', &^'c'
//-------------------------------------------------------------------
func (p *ParserBase) AheadNotRune(ch rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)==ch){
			return p.fail(fmt.Sprintf("not '%v'",ch));
		} else {
			return true;
		}
	}


//-------------------------------------------------------------------
//  Execute expression "s"
//-------------------------------------------------------------------
func (p *ParserBase) NextStr(s string) bool {
		lg:=utf8.RuneCountInString(s);
		if (p.pos+lg<=p.endpos && p.source.At(p.pos,p.pos+lg)==s) {
			return p.consume(lg);
		} else {
			return p.fail(fmt.Sprintf("'%v'",s));
		}
	}

//-------------------------------------------------------------------
//  Execute expression &"s"
//-------------------------------------------------------------------
func (p *ParserBase) AheadStr(s string) bool {
		lg:=utf8.RuneCountInString(s);
		if (p.pos+lg<=p.endpos && p.source.At(p.pos,p.pos+lg)==s) {
			return true;
		} else {
			return p.fail(fmt.Sprintf("'%v'",s));
		}
	}

//-------------------------------------------------------------------
//  Execute expression !"s"
//-------------------------------------------------------------------
func (p *ParserBase) AheadNotStr(s string) bool {
		lg:=utf8.RuneCountInString(s);
		if (p.pos+lg<=p.endpos && p.source.At(p.pos,p.pos+lg)==s){
			return p.fail(fmt.Sprintf("not '%v'",s));
		} else {
			return true;
		}
	}


//-------------------------------------------------------------------
//  Execute expression [s]
//-------------------------------------------------------------------
func (p *ParserBase) NextInStr(s string) bool {
		if (p.pos<p.endpos && strings.ContainsRune(s,p.source.RuneAt(p.pos))){
			return p.consume(1);
		} else {
			return p.fail(fmt.Sprintf("[%v]",s));
		}
	}

//-------------------------------------------------------------------
//  Execute expression ^[s]
//-------------------------------------------------------------------
func (p *ParserBase) NextNotInStr(s string) bool {
		if (p.pos<p.endpos && !strings.ContainsRune(s,p.source.RuneAt(p.pos))){
			return p.consume(1);
		} else {
			return p.fail(fmt.Sprintf("not [%v]",s));
		}
	}

//-------------------------------------------------------------------
//  Execute expression &[s], !^[s]
//-------------------------------------------------------------------
func (p *ParserBase) AheadInStr(s string) bool {
		if (p.pos<p.endpos && strings.ContainsRune(s,p.source.RuneAt(p.pos))){
			return true;
		} else {
			return p.fail(fmt.Sprintf("[%v]",s));
		}
	}

func (p *ParserBase) AheadNotNotIn(s string) bool { return p.AheadInStr(s); }

//-------------------------------------------------------------------
//  Execute expression ![s], &^[s]
//-------------------------------------------------------------------
func (p *ParserBase) AheadNotInStr(s string) bool {
		if (p.pos<p.endpos && !strings.ContainsRune(s,p.source.RuneAt(p.pos))){
			return p.fail(fmt.Sprintf("not [%v]",s));
		} else {
			return true;
		}
	}


//-------------------------------------------------------------------
//  Execute expression [a-z]
//-------------------------------------------------------------------
func (p *ParserBase) NextIn(a rune, z rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)>=a && p.source.RuneAt(p.pos)<=z){
			return p.consume(1);
		} else {
			return p.fail(fmt.Sprintf("[%c-%c]",a,z));
		}
	}

//-------------------------------------------------------------------
//  Execute expression &[a-z]
//-------------------------------------------------------------------
func (p *ParserBase) AheadIn(a rune, z rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)>=a && p.source.RuneAt(p.pos)<=z){
			return true;
		} else {
			return p.fail(fmt.Sprintf("[%c-%c]",a,z));
		}
	}

//-------------------------------------------------------------------
//  Execute expression ![a-z]
//-------------------------------------------------------------------
func (p *ParserBase) AheadNotIn(a rune,z rune) bool {
		if (p.pos<p.endpos && p.source.RuneAt(p.pos)>=a && p.source.RuneAt(p.pos)<=z){
			return p.fail(fmt.Sprintf("not [%c-%c]",a,z));
		} else {
			return true;
		}
	}


//-------------------------------------------------------------------
//  Execute expression _
//-------------------------------------------------------------------
func (p *ParserBase) Next() bool {
		if (p.pos<p.endpos){
			 return p.consume(1);
		} else {
			return p.fail("any character");
		}
	}

//-------------------------------------------------------------------
//  Execute expression &_
//-------------------------------------------------------------------
func (p *ParserBase) Ahead() bool {
		if (p.pos<p.endpos){
			return true;
		} else {
			return p.fail("any character");
		}
	}

//-------------------------------------------------------------------
//  Execute expression !_
//-------------------------------------------------------------------
func (p *ParserBase) AheadNot() bool {
		if (p.pos<p.endpos){
			return p.fail("end of text");
		} else {
			return true;
		}
	}


//-------------------------------------------------------------------
//  Consume terminal
//-------------------------------------------------------------------
func (p *ParserBase) consume(n int) bool {
		phrase:= NewPhrase(p,"","",p.pos);
		p.pos += n;
		phrase.end = p.pos;
		p.current.rhs=append(p.current.rhs,phrase);
		p.current.end = p.pos;
		return true;
	}

//-------------------------------------------------------------------
//  Fail
//-------------------------------------------------------------------
func (p *ParserBase) fail(msg string) bool {
		p.current.ErrAdd(msg);
		return false;
}


func (p *ParserBase) pop() *Phrase {
	phrase:=p.current;
	p.current = phrase.parent;
	phrase.parent = nil;
	return phrase;
}

