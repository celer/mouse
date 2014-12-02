package mouse;

import "fmt"

type ParserMemo struct {
	ParserBase;
	cacheSize int;
	reuse *Phrase;
	Caches []*Cache;
}

func NewParserMemo() *ParserMemo {
	p:=ParserMemo{};
	p.cacheSize=-1;
	p.init();
	return &p;
}

func (p *ParserMemo) init() {
	for _,c:=range p.Caches {
		c.reset(p.cacheSize);
	}
}

func (p *ParserMemo) SetMemo(m int){
	if (m<0 || m>9){ panic(fmt.Sprintf("m=%d is outside range 0-9",m)); }
	p.cacheSize = m;
}

//=====================================================================
//
//  Methods called from parsing procedures
//
//=====================================================================
//-------------------------------------------------------------------
//  If saved result found, use it, otherwise begin new procedure.
//  Version for Rule.
//-------------------------------------------------------------------
func (p *ParserMemo) Saved(c *Cache) bool {
		p.reuse = c.find(p.pos,p.cacheSize);
		if (p.reuse!=nil){                // If found Phrase to reuse..
			return true;                   // .. return	
		}

		p.BeginWithDiag(c.name,c.diag);          // Otherwise push new Phrase
		c.save(p.current,p.cacheSize);                 // .. and cache it
		return false;
	}

//-------------------------------------------------------------------
//  If saved result found, use it, otherwise begin new procedure.
//  Version for Inner.
//-------------------------------------------------------------------
func (p *ParserMemo) SavedInner(c *Cache) bool {
		p.reuse = c.find(p.pos,p.cacheSize);
		if (p.reuse!=nil){               // If found Phrase to reuse..
			return true;                   // .. return
		}

		p.BeginWithDiag("",c.diag);              // Otherwise push new Phrase
		c.save(p.current,p.cacheSize);                 // .. and cache it
		return false;
	}

//-------------------------------------------------------------------
//  Reuse Rule
//-------------------------------------------------------------------
func (p *ParserMemo) Reuse() bool {
		if (p.reuse.success) {
			p.pos = p.reuse.end;               // Update position
			p.current.end = p.pos;             // Update end of current
			p.current.rhs=append(p.current.rhs,p.reuse);        // Attach p to rhs of current
			p.current.errMerge(p.reuse);       // Merge error info with current
			return true;
		} else {
			p.current.errMerge(p.reuse);       // Merge error info with current
			return false;
		}
	}

//-------------------------------------------------------------------
//  Reuse Inner
//-------------------------------------------------------------------
func (p *ParserMemo) ReuseInner() bool {
		if (p.reuse.success) {
			p.pos = p.reuse.end;               // Update position
			p.current.end = p.pos;             // Update end of current
			for _,r:=range p.reuse.rhs {
				p.current.rhs=append(p.current.rhs,r);	//Add rhs to rhs of current
			}
			p.current.errMerge(p.reuse);       // Merge error info with current
			return true;
		} else {
			p.current.errMerge(p.reuse);       // Merge error info with current
			return false;
		}
	}

//-------------------------------------------------------------------
//  Reuse predicate
//-------------------------------------------------------------------
func (p *ParserMemo) ReusePred() bool {
		if (p.reuse.success){
			return true;
		} else {
			p.current.errMerge(p.reuse);       // Merge error info with current
			return false;
		}
}

